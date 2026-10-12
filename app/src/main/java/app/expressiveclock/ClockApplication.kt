package app.expressiveclock

import android.app.Application
import app.expressiveclock.overlay.AppLifecycleTracker
import app.expressiveclock.overlay.OverlayManager

class ClockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppLifecycleTracker)
        OverlayManager.init(this)
    }
}
