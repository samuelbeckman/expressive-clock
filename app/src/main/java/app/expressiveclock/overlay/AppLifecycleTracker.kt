package app.expressiveclock.overlay

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Tracks whether the Clock application is currently in the foreground or background.
 *
 * When the user is actively using the Clock app UI, floating overlays are hidden because they
 * would be redundant over our own full-screen pages.
 */
object AppLifecycleTracker : Application.ActivityLifecycleCallbacks {

    var isAppInForeground: Boolean = false
        private set

    var hasObservedFirstActivityState: Boolean = false
        private set

    private var startedActivityCount = 0

    override fun onActivityStarted(activity: Activity) {
        hasObservedFirstActivityState = true
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        hasObservedFirstActivityState = true
        startedActivityCount++
        if (!isAppInForeground) {
            isAppInForeground = true
            OverlayManager.onClockActivityForegrounded()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        if (startedActivityCount == 0 && isAppInForeground) {
            isAppInForeground = false
            OverlayManager.onAppForegroundChanged(false)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
