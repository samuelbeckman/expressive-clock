package app.expressiveclock.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.expressiveclock.R
import app.expressiveclock.alarm.Notifications

/**
 * Foreground service to ensure the process remains alive while the Dynamic Island overlay is active.
 */
class OverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Dynamic Island Overlay")
            .setContentText("Timer/Stopwatch overlay active")
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(SERVICE_ID, notification)
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val SERVICE_ID = 404

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            runCatching {
                context.stopService(intent)
            }
        }
    }
}
