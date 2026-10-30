package app.expressiveclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.expressiveclock.core.TimerState
import app.expressiveclock.data.ClockStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles exact alarm firings and timer expiries from `AlarmManager`.
 *
 * ## `goAsync`
 *
 * Reading DataStore suspends and `onReceive` runs on the main thread with a hard deadline. The
 * pending result keeps the process alive across the coroutine; `finish()` must be reached on every
 * path or the receiver leaks and the system eventually kills the app for it.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> fire(app, intent.getLongExtra(EXTRA_ID, -1L))
                    ACTION_TIMER_EXPIRED -> timerExpired(app)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(context: Context, id: Long) {
        val store = ClockStore(context)
        val alarm = store.alarmsNow().firstOrNull { it.id == id } ?: return

        // Clearing the snooze first matters: if the ring is what a snooze produced, leaving the
        // stamp in place would make `nextFire` keep returning a moment in the past forever.
        val cleared = store.alarmsNow().map { a ->
            when {
                a.id != id -> a
                // A one-shot has now happened. Disarm it rather than leave a dead row armed. That
                // holds *including* when this ring came from a snooze, the case an earlier version
                // missed: the snooze is that alarm's last occurrence, not an extra one, so
                // a one-shot snoozed at 06:45 and then dismissed at 06:55 must end up off. Pressing
                // Snooze again re-arms it, which is what ClockActionReceiver does deliberately.
                a.isOneShot -> a.copy(enabled = false, snoozedUntilMillis = null)
                else -> a.copy(snoozedUntilMillis = null)
            }
        }
        store.putAlarms(cleared)
        cleared.firstOrNull { it.id == id }?.let { AlarmScheduler.schedule(context, it) }

        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmService::class.java)
                .setAction(AlarmService.ACTION_START)
                .putExtra(EXTRA_ID, alarm.id),
        )
    }

    private suspend fun timerExpired(context: Context) {
        val store = ClockStore(context)
        val timer = store.timerNow() ?: return
        // Mark the timer as FINISHED so the full-screen finished screen is presented in the app
        store.putTimer(timer.copy(state = TimerState.FINISHED))
        Notifications.hideTimer(context)
        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_START_TIMER),
        )
    }

    companion object {
        const val ACTION_FIRE = "app.expressiveclock.FIRE"
        const val ACTION_TIMER_EXPIRED = "app.expressiveclock.TIMER_EXPIRED"
        const val EXTRA_ID = "id"
    }
}
