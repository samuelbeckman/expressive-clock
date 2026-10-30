package app.expressiveclock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import app.expressiveclock.data.ClockStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Provider for the Digital Clock widget.
 *
 * Uses `TextClock` views for zero-wakeup time and date updates natively in the host launcher.
 */
class DigitalClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        background(context) { store ->
            ids.forEach { DigitalClockWidgetRenderer.update(context, manager, it) }
            store.sweepWidgetConfigs(ClockWidgetProvider.allPlacedWidgetIds(context))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        background(context) { DigitalClockWidgetRenderer.update(context, manager, appWidgetId) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        background(context) { it.deleteWidgetConfigs(ids) }
    }

    override fun onRestored(context: Context, oldIds: IntArray, newIds: IntArray) {
        background(context) { store ->
            store.remapWidgetConfigs(oldIds, newIds)
            val manager = AppWidgetManager.getInstance(context)
            newIds.forEach { DigitalClockWidgetRenderer.update(context, manager, it) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> refreshAll(context)
        }
    }

    private fun background(context: Context, work: suspend (ClockStore) -> Unit) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                work(ClockStore(app))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun component(context: Context) = ComponentName(context, DigitalClockWidgetProvider::class.java)

        fun refreshAll(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(component(app))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.Default).launch {
                ids.forEach { DigitalClockWidgetRenderer.update(app, manager, it) }
            }
        }

        fun placedIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context).getAppWidgetIds(component(context))
    }
}
