package app.expressiveclock.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.expressiveclock.data.ClockSettings
import app.expressiveclock.data.ClockStore
import app.expressiveclock.data.WidgetConfig
import app.expressiveclock.data.WidgetType
import app.expressiveclock.ui.theme.ClockTheme
import kotlinx.coroutines.launch

/**
 * The editor that appears when a clock widget is placed, and again when it is reconfigured.
 *
 * ## The contract, which is unforgiving
 *
 * The launcher builds this intent with **no** `FLAG_ACTIVITY_NEW_TASK` and calls
 * `startIntentSenderForResult`, so the activity runs *inside the launcher's own task*. It therefore
 * must not declare `launchMode`, `taskAffinity`, `noHistory` or a translucent theme. Copying
 * `MainActivity`'s `singleTask` would force a separate task, return `RESULT_CANCELED` immediately,
 * and delete the widget before the user saw anything.
 *
 * `RESULT_CANCELED` is set in `onCreate`, before any UI: with targetSdk 36 predictive back is on
 * and `onBackPressed` is no longer called, so there is no later opportunity.
 *
 * ## Commit order
 *
 * The order is persist, render, `setResult(RESULT_OK)`, finish, and every step is awaited. Any
 * death before `setResult` reads as a cancel, the host drops the id, and `onDeleted` clears the
 * half-written record. It is the only ordering with no inconsistent outcome.
 *
 * The draft is held in memory until Save, so backing out of a *reconfigure* leaves the previous
 * configuration untouched.
 */
class ClockWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Before setContent, per the contract above.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // The activity is exported, so anyone can launch it with any id. Only act on ids that
        // really belong to our widget providers.
        val manager = AppWidgetManager.getInstance(this)
        val provider = manager.getAppWidgetInfo(appWidgetId)?.provider
        val isDigital = provider == DigitalClockWidgetProvider.component(this)
        val isAnalog = provider == ClockWidgetProvider.component(this)

        if (!isDigital && !isAnalog) {
            finish()
            return
        }

        val store = ClockStore(applicationContext)
        val defaultType = if (isDigital) WidgetType.DIGITAL else WidgetType.ANALOG

        lifecycleScope.launch {
            val savedConfig = store.widgetConfigNow(appWidgetId)
            val initialConfig = savedConfig ?: WidgetConfig(type = defaultType)

            setContent {
                val settings by store.settings.collectAsStateWithLifecycle(initialValue = ClockSettings())

                ClockTheme(settings.theme) {
                    WidgetConfigScreen(
                        initial = initialConfig,
                        settings = settings,
                        isNew = savedConfig == null,
                        onCancel = { finish() },
                        onSave = { config -> commit(appWidgetId, config) },
                    )
                }
            }
        }
    }

    private fun commit(appWidgetId: Int, config: WidgetConfig) {
        lifecycleScope.launch {
            val store = ClockStore(applicationContext)
            store.putWidgetConfig(appWidgetId, config)
            val manager = AppWidgetManager.getInstance(applicationContext)
            if (config.type == WidgetType.DIGITAL) {
                DigitalClockWidgetRenderer.update(applicationContext, manager, appWidgetId)
            } else {
                ClockWidgetRenderer.update(applicationContext, manager, appWidgetId)
            }
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}
