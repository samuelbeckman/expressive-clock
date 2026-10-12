package app.expressiveclock.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Service used solely to host the Clock app's Dynamic Island overlay at the
 * TYPE_ACCESSIBILITY_OVERLAY system window layer above status bars.
 *
 * Does NOT inspect window content, read screens, or perform gestures.
 */
class OverlayAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        OverlayManager.onAccessibilityServiceConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty: no event processing or screen inspection.
    }

    override fun onInterrupt() {
        // Intentionally empty.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        OverlayManager.onAccessibilityServiceDisconnected()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        OverlayManager.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }

    companion object {
        var instance: OverlayAccessibilityService? = null
            private set
    }
}
