package app.expressiveclock.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.expressiveclock.MainActivity
import app.expressiveclock.alarm.ClockActionReceiver
import app.expressiveclock.alarm.Notifications
import app.expressiveclock.core.ClockTimer
import app.expressiveclock.core.Stopwatch
import app.expressiveclock.core.TimerState
import app.expressiveclock.data.ClockSettings
import app.expressiveclock.data.ClockStore
import app.expressiveclock.ui.theme.ClockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.roundToInt

/**
 * Controller for adding, updating, and removing the Dynamic Island overlay via WindowManager.
 *
 * Architecture:
 * - Layer 1 (Rendering Window): Full-screen MATCH_PARENT window with FLAG_NOT_TOUCHABLE for rendering smooth Compose animations.
 * - Layer 2 (Interaction Window): Bounded window matching activeIslandBounds for precise touch interaction.
 */
object OverlayManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayWindowManager: WindowManager? = null
    private var renderComposeView: ComposeView? = null
    private var interactionComposeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var overlayViewModelStoreOwner: ViewModelStoreOwner? = null
    private var appContext: Context? = null

    private var currentSettings: ClockSettings? = null
    private var currentTimer: ClockTimer? = null
    private var currentStopwatch: Stopwatch? = null

    // Persistent observable Compose states for the single composition tree
    private var overlayTimerState by mutableStateOf<ClockTimer?>(null)
    private var overlayStopwatchState by mutableStateOf<Stopwatch?>(null)
    private var overlaySettingsState by mutableStateOf(ClockSettings())
    private var overlayIsExitingState by mutableStateOf(false)
    private var overlayIsCalibrationState by mutableStateOf(false)
    private var overlayIsExpandedState by mutableStateOf(false)

    // Dedicated exit request ID and visual snapshot states
    private var overlayExitRequestId by mutableStateOf(0)
    private var overlayExitTimerState: ClockTimer? by mutableStateOf(null)
    private var overlayExitStopwatchState: Stopwatch? by mutableStateOf(null)

    private var isOverlayAttached = false
    private var isOverlayBeingRemoved = false
    private var isCalibrationPreviewing = false
    private var activeIslandBounds: Rect? = null
    private var overlayGeneration = 0

    // Queued app launch state to prevent foreground lifecycle crashes
    private var pendingAppLaunchTab: String? = null
    private var pendingExitAfterCollapse = false

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        windowManager = app.getSystemService<WindowManager>()

        val store = ClockStore(app)
        scope.launch {
            combine(store.settings, store.timer, store.stopwatch) { s, t, sw ->
                Triple(s, t, sw)
            }.collect { (s, t, sw) ->
                currentSettings = s
                currentTimer = t
                currentStopwatch = sw
                syncOverlayState()
            }
        }
    }

    fun onAccessibilityServiceConnected(service: OverlayAccessibilityService) {
        val wm = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm != null) {
            windowManager = wm
            Log.d("DynamicIslandOverlay", "onAccessibilityServiceConnected: windowManager set (wmHash=${System.identityHashCode(wm)})")
        }
        syncOverlayState()
    }

    fun onAccessibilityServiceDisconnected() {
        Log.d("DynamicIslandOverlay", "onAccessibilityServiceDisconnected: service disconnected (overlayAttached=$isOverlayAttached, force=true)")
        if (isOverlayAttached) {
            removeOverlayNow(force = true)
        }
        windowManager = null
    }

    fun showCalibrationPreview(context: Context, settings: ClockSettings) {
        currentSettings = settings
        isCalibrationPreviewing = true
        syncOverlayState()
    }

    fun updateCalibrationPreview(settings: ClockSettings) {
        currentSettings = settings
        overlaySettingsState = settings
        if (isOverlayAttached) {
            updateWindowParams()
        } else {
            syncOverlayState()
        }
    }

    fun hideCalibrationPreview() {
        isCalibrationPreviewing = false
        overlayIsCalibrationState = false
        syncOverlayState()
    }

    fun onClockActivityForegrounded() {
        syncOverlayState()
    }

    fun hideImmediatelyForAppForeground() {
        if (!isOverlayAttached) return
        Log.d("DynamicIslandOverlay", "OVERLAY DETACH START (gen=$overlayGeneration, wmHash=${System.identityHashCode(overlayWindowManager)})")
        removeOverlayNow(force = true)
        Log.d("DynamicIslandOverlay", "OVERLAY DETACH COMPLETE")
    }

    fun onAppForegroundChanged(inForeground: Boolean) {
        syncOverlayState()
    }

    fun requestOpenAppAfterAnimatedExit(tab: String) {
        Log.d("DynamicIslandOverlay", "DI APP OPEN REQUESTED tab=$tab")
        val context = appContext ?: return
        launchMainActivityNow(context, tab)
        if (overlayIsExpandedState) {
            overlayIsExpandedState = false
            updateWindowParams()
        }
    }

    private fun launchMainActivityNow(context: Context, tab: String) {
        Log.d("DynamicIslandOverlay", "DI LAUNCHING MAIN ACTIVITY tab=$tab")
        runCatching {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(Notifications.EXTRA_TAB, tab)
            }
            context.startActivity(intent)
        }.onFailure { Log.e("OverlayManager", "Failed to launch MainActivity", it) }
    }

    fun syncOverlayState() {
        val context = appContext ?: return

        // Cold start guard: Wait until AppLifecycleTracker has observed first activity lifecycle state
        if (!AppLifecycleTracker.hasObservedFirstActivityState && !isCalibrationPreviewing) {
            Log.d("DynamicIslandOverlay", "syncOverlayState deferred: Waiting for first activity lifecycle state")
            return
        }

        val settings = currentSettings ?: ClockSettings()
        val isEnabledInSettings = settings.theme.dynamicIsland
        val isAccConnected = OverlayAccessibilityService.instance != null

        overlaySettingsState = settings
        overlayIsCalibrationState = isCalibrationPreviewing

        val timer = currentTimer ?: if (isCalibrationPreviewing) ClockTimer(id = -1, label = "Preview", total = Duration.ofMinutes(5), state = TimerState.RUNNING) else null
        val sw = currentStopwatch

        val isTimerActive = timer != null && timer.state != TimerState.FINISHED
        val isStopwatchActive = sw != null && (sw.running || !sw.accumulated.isZero)
        val shouldShow = isCalibrationPreviewing || ((isTimerActive || isStopwatchActive) && isEnabledInSettings && isAccConnected)

        Log.d("DynamicIslandOverlay", "DI SYNC STATE: shouldShow=$shouldShow, isTimerActive=$isTimerActive, isStopwatchActive=$isStopwatchActive, isAttached=$isOverlayAttached, isExiting=$overlayIsExitingState")

        if (shouldShow) {
            if (overlayIsExitingState && !isCalibrationPreviewing) {
                Log.d("DynamicIslandOverlay", "syncOverlayState: Ignoring show request because animated exit is in progress")
                return
            }
            overlayTimerState = timer
            overlayStopwatchState = sw
            overlayIsExitingState = false
            overlayExitRequestId = 0
            if (!isCalibrationPreviewing) {
                runCatching {
                    OverlayService.start(context)
                }
            }
            showOrUpdateOverlay(context)
        } else {
            if (isOverlayAttached && !overlayIsExitingState) {
                Log.d("DynamicIslandOverlay", "DI HIDE ROUTE = ANIMATED_EXIT")
                requestAnimatedOverlayHide()
            } else if (!isOverlayAttached) {
                overlayTimerState = timer
                overlayStopwatchState = sw
                removeOverlayNow(force = false)
            }
        }
    }

    fun requestAnimatedOverlayHide() {
        if (!isOverlayAttached || overlayIsExitingState) return

        if (overlayIsExpandedState) {
            Log.d("DynamicIslandOverlay", "DI COLLAPSE BEFORE CAMERA EXIT")
            pendingExitAfterCollapse = true

            val interactionView = interactionComposeView
            val wm = overlayWindowManager ?: windowManager
            if (interactionView != null && wm != null) {
                interactionComposeView = null
                runCatching {
                    wm.removeViewImmediate(interactionView)
                }
            }

            overlayIsExpandedState = false
            updateWindowParams()
            return
        }

        overlayExitRequestId++
        overlayExitTimerState = overlayTimerState
        overlayExitStopwatchState = overlayStopwatchState
        overlayIsExitingState = true
        Log.d("DynamicIslandOverlay", "DI BEGIN ANIMATED EXIT request=$overlayExitRequestId")
        Log.d("DynamicIslandOverlay", "DI EXIT SNAPSHOT timer=$overlayExitTimerState stopwatch=$overlayExitStopwatchState")

        // Remove Layer 2 interaction window immediately so user cannot touch the shrinking island
        val interactionView = interactionComposeView
        val wm = overlayWindowManager ?: windowManager
        if (interactionView != null && wm != null) {
            interactionComposeView = null
            runCatching {
                wm.removeViewImmediate(interactionView)
            }.onFailure { Log.e("OverlayManager", "Failed to remove interaction view during exit start", it) }
        }
    }

    private fun triggerExitAnimation() {
        requestAnimatedOverlayHide()
    }

    private fun showOrUpdateOverlay(context: Context) {
        val service = OverlayAccessibilityService.instance
        if (service == null && !isCalibrationPreviewing) {
            removeOverlayNow(force = true)
            return
        }
        val wm = windowManager ?: return

        if (renderComposeView == null) {
            overlayGeneration++
            val currentGen = overlayGeneration

            val lifecycleOwner = OverlayLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            overlayLifecycleOwner = lifecycleOwner

            val viewModelStoreOwner = object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }
            overlayViewModelStoreOwner = viewModelStoreOwner

            // Layer 1: Rendering View (Full-screen, non-touchable)
            val renderView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setContent {
                    PersistentOverlayContent(context, currentGen)
                }
            }
            renderComposeView = renderView

            val renderParams = createRenderParams() ?: return
            val addResult = runCatching {
                wm.addView(renderView, renderParams)
                isOverlayAttached = true
                overlayWindowManager = wm
            }
            if (addResult.isFailure) {
                Log.e("OverlayManager", "Failed to add render overlay window (gen=$currentGen, wmHash=${System.identityHashCode(wm)})", addResult.exceptionOrNull())
                renderComposeView = null
                isOverlayAttached = false
                overlayWindowManager = null
                activeIslandBounds = null
            } else {
                Log.d("DynamicIslandOverlay", "Render overlay window added successfully (gen=$currentGen, wmHash=${System.identityHashCode(wm)})")
            }
        } else {
            updateWindowParams()
        }

        // Layer 2: Interaction Window (Bounded, touchable)
        if (isOverlayAttached && !isCalibrationPreviewing) {
            val attachedWm = overlayWindowManager ?: wm
            showOrUpdateInteractionView(context, attachedWm, overlayGeneration)
        } else if (isCalibrationPreviewing && interactionComposeView != null) {
            val attachedWm = overlayWindowManager ?: wm
            runCatching {
                attachedWm.removeViewImmediate(interactionComposeView)
            }
            interactionComposeView = null
        }
    }

    private fun showOrUpdateInteractionView(context: Context, wm: WindowManager, generation: Int) {
        val attachedWm = overlayWindowManager ?: wm
        if (!isOverlayAttached || generation != overlayGeneration || overlayIsExitingState) return
        val bounds = activeIslandBounds ?: return
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val params = createInteractionParams(bounds) ?: return

        if (interactionComposeView == null) {
            val lifecycleOwner = overlayLifecycleOwner ?: OverlayLifecycleOwner().apply {
                performRestore(null)
                handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                handleLifecycleEvent(Lifecycle.Event.ON_START)
                handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
            val viewModelStoreOwner = overlayViewModelStoreOwner ?: object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_OUTSIDE && overlayIsExpandedState) {
                        overlayIsExpandedState = false
                        updateWindowParams()
                    }
                    false
                }

                setContent {
                    InteractionOverlayContent(context)
                }
            }
            interactionComposeView = view

            val addResult = runCatching {
                attachedWm.addView(view, params)
            }
            if (addResult.isFailure) {
                Log.e("OverlayManager", "Failed to add interaction window (gen=$generation, wmHash=${System.identityHashCode(attachedWm)})", addResult.exceptionOrNull())
                interactionComposeView = null
            } else {
                Log.d("DynamicIslandOverlay", "Interaction window added successfully (gen=$generation, wmHash=${System.identityHashCode(attachedWm)})")
            }
        } else {
            val updateResult = runCatching {
                attachedWm.updateViewLayout(interactionComposeView, params)
            }
            if (updateResult.isFailure) {
                Log.e("OverlayManager", "Failed to update interaction window params (gen=$generation, wmHash=${System.identityHashCode(attachedWm)})", updateResult.exceptionOrNull())
            }
        }
    }

    @Composable
    private fun PersistentOverlayContent(context: Context, generation: Int) {
        val settings = overlaySettingsState
        val visualTimer = if (overlayIsExitingState) overlayExitTimerState else overlayTimerState
        val visualStopwatch = if (overlayIsExitingState) overlayExitStopwatchState else overlayStopwatchState

        ClockTheme {
            DynamicIslandOverlayContent(
                timer = visualTimer,
                stopwatch = visualStopwatch,
                exitRequestId = overlayExitRequestId,
                topInsetDp = 8.dp,
                cutoutWidthDp = 48.dp,
                offsetXDp = settings.theme.overlayOffsetX.dp,
                offsetYDp = settings.theme.overlayOffsetY.dp,
                widthOffsetDp = settings.theme.overlayWidthOffset.dp,
                heightOffsetDp = settings.theme.overlayHeightOffset.dp,
                isCalibrationPreview = overlayIsCalibrationState,
                isExiting = overlayIsExitingState,
                isExpanded = overlayIsExpandedState,
                onBoundsChanged = { bounds ->
                    if (isOverlayAttached && generation == overlayGeneration) {
                        activeIslandBounds = bounds
                        val attachedWm = overlayWindowManager ?: windowManager
                        if (attachedWm != null && !isCalibrationPreviewing) {
                            showOrUpdateInteractionView(context, attachedWm, generation)
                        }
                    }
                },
                onCollapsedSettled = {
                    if (generation == overlayGeneration && pendingExitAfterCollapse) {
                        Log.d("DynamicIslandOverlay", "DI COLLAPSE COMPLETE")
                        pendingExitAfterCollapse = false
                        requestAnimatedOverlayHide()
                    }
                },
                onExitComplete = {
                    if (generation == overlayGeneration) {
                        Log.d("DynamicIslandOverlay", "DI EXIT COMPLETE -> REMOVING (force=true, gen=$generation)")
                        Handler(Looper.getMainLooper()).post {
                            if (generation == overlayGeneration) {
                                removeOverlayNow(force = true)
                            }
                        }
                    }
                },
                onExpandChanged = { expanded ->
                    if (generation == overlayGeneration) {
                        overlayIsExpandedState = expanded
                        updateWindowParams()
                    }
                },
                onOpenAppTab = { tab ->
                    if (generation == overlayGeneration) {
                        requestOpenAppAfterAnimatedExit(tab)
                    }
                },
                onPauseResumeTimer = { sendAction(context, ClockActionReceiver.ACTION_TIMER_TOGGLE) },
                onAddTenTimer = { sendAction(context, ClockActionReceiver.ACTION_TIMER_ADD) },
                onCancelTimer = { sendAction(context, ClockActionReceiver.ACTION_TIMER_CANCEL) },
                onToggleStopwatch = { sendAction(context, ClockActionReceiver.ACTION_SW_TOGGLE) },
                onLapStopwatch = { sendAction(context, ClockActionReceiver.ACTION_SW_LAP) },
            )
        }
    }

    @Composable
    private fun InteractionOverlayContent(context: Context) {
        val isTimerActive = overlayTimerState != null && overlayTimerState?.state != TimerState.FINISHED
        ClockTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (overlayIsExpandedState) {
                            val tab = if (isTimerActive) Notifications.TAB_TIMERS else Notifications.TAB_STOPWATCH
                            requestOpenAppAfterAnimatedExit(tab)
                        } else if (!overlayIsExitingState) {
                            overlayIsExpandedState = true
                            updateWindowParams()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (overlayIsExpandedState) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isTimerActive) {
                                // Button 1: Pause / Resume
                                InteractionScallopedButton(
                                    onClick = { sendAction(context, ClockActionReceiver.ACTION_TIMER_TOGGLE) },
                                )
                                // Button 2: +1m
                                InteractionScallopedButton(
                                    onClick = { sendAction(context, ClockActionReceiver.ACTION_TIMER_ADD) },
                                )
                                // Button 3: Cancel
                                InteractionScallopedButton(
                                    onClick = { sendAction(context, ClockActionReceiver.ACTION_TIMER_CANCEL) },
                                )
                            } else {
                                // Button 1: Pause / Resume
                                InteractionScallopedButton(
                                    onClick = { sendAction(context, ClockActionReceiver.ACTION_SW_TOGGLE) },
                                )
                                // Button 2: Lap
                                InteractionScallopedButton(
                                    onClick = { sendAction(context, ClockActionReceiver.ACTION_SW_LAP) },
                                )
                                // Button 3: Cancel/Reset
                                InteractionScallopedButton(
                                    onClick = { sendAction(context, ClockActionReceiver.ACTION_SW_RESET) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InteractionScallopedButton(
        onClick: () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
        )
    }

    private fun updateWindowParams() {
        val view = renderComposeView ?: return
        val wm = overlayWindowManager ?: windowManager ?: return
        if (!isOverlayAttached) return
        val params = createRenderParams() ?: return
        runCatching {
            wm.updateViewLayout(view, params)
        }.onFailure { Log.e("OverlayManager", "Failed to update render window params (wmHash=${System.identityHashCode(wm)})", it) }

        if (interactionComposeView != null) {
            val interactionParams = activeIslandBounds?.let { createInteractionParams(it) }
            if (interactionParams != null) {
                runCatching {
                    wm.updateViewLayout(interactionComposeView, interactionParams)
                }.onFailure { Log.e("OverlayManager", "Failed to update interaction window params (wmHash=${System.identityHashCode(wm)})", it) }
            }
        }
    }

    private fun createRenderParams(): WindowManager.LayoutParams? {
        val service = OverlayAccessibilityService.instance
        if (service == null && !isCalibrationPreviewing) {
            return null
        }

        val settings = currentSettings ?: ClockSettings()
        val density = appContext?.resources?.displayMetrics?.density ?: 1f
        val userX = (settings.theme.overlayOffsetX * density).roundToInt()
        val userY = (settings.theme.overlayOffsetY * density).roundToInt()

        // FLAG_NOT_TOUCHABLE guarantees Layer 1 never blocks touches outside the island!
        val touchFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            touchFlags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = userX
            y = userY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun createInteractionParams(bounds: Rect): WindowManager.LayoutParams? {
        val service = OverlayAccessibilityService.instance
        if (service == null && !isCalibrationPreviewing) {
            return null
        }

        val touchFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        return WindowManager.LayoutParams(
            bounds.width().coerceAtLeast(100),
            bounds.height().coerceAtLeast(40),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            touchFlags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left
            y = bounds.top
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun removeOverlayNow(force: Boolean = false) {
        if (isOverlayBeingRemoved) return

        if (overlayIsExitingState && !force) {
            Log.d("DynamicIslandOverlay", "DI REMOVE IGNORED force=false while overlayIsExitingState=true (gen=$overlayGeneration)")
            return
        }

        Log.d("DynamicIslandOverlay", "DI REMOVE force=$force exiting=$overlayIsExitingState (gen=$overlayGeneration)")
        isOverlayBeingRemoved = true

        val pendingTab = pendingAppLaunchTab
        pendingAppLaunchTab = null
        pendingExitAfterCollapse = false
        overlayExitTimerState = null
        overlayExitStopwatchState = null
        overlayExitRequestId = 0

        overlayGeneration++
        val renderView = renderComposeView
        val interactionView = interactionComposeView
        val wm = overlayWindowManager ?: windowManager
        val lifecycleOwner = overlayLifecycleOwner
        val vmStoreOwner = overlayViewModelStoreOwner

        renderComposeView = null
        interactionComposeView = null
        overlayLifecycleOwner = null
        overlayViewModelStoreOwner = null
        overlayWindowManager = null
        isOverlayAttached = false
        overlayIsExpandedState = false
        overlayIsExitingState = false
        activeIslandBounds = null

        try {
            if (interactionView != null && wm != null) {
                val removeResult = runCatching {
                    wm.removeViewImmediate(interactionView)
                }
                if (removeResult.isFailure) {
                    Log.e("OverlayManager", "Failed to remove interaction view (wmHash=${System.identityHashCode(wm)})", removeResult.exceptionOrNull())
                } else {
                    Log.d("DynamicIslandOverlay", "DI INTERACTION WINDOW REMOVED")
                }

                runCatching {
                    interactionView.disposeComposition()
                }.onFailure { Log.e("OverlayManager", "Failed to dispose interaction composition", it) }
            }

            if (renderView != null && wm != null) {
                val removeResult = runCatching {
                    wm.removeViewImmediate(renderView)
                }
                if (removeResult.isFailure) {
                    Log.e("OverlayManager", "Failed to remove render view (wmHash=${System.identityHashCode(wm)})", removeResult.exceptionOrNull())
                } else {
                    Log.d("DynamicIslandOverlay", "DI RENDER WINDOW REMOVED")
                }

                runCatching {
                    renderView.disposeComposition()
                }.onFailure { Log.e("OverlayManager", "Failed to dispose render composition", it) }
            }

            if (lifecycleOwner != null) {
                runCatching {
                    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                }.onFailure { Log.e("OverlayManager", "Failed to destroy overlay lifecycle", it) }
            }

            if (vmStoreOwner != null) {
                runCatching {
                    vmStoreOwner.viewModelStore.clear()
                }.onFailure { Log.e("OverlayManager", "Failed to clear overlay view model store", it) }
            }

            Log.d("DynamicIslandOverlay", "DI COMPOSITIONS DISPOSED & OVERLAY DETACH COMPLETE")

            if (pendingTab != null) {
                Handler(Looper.getMainLooper()).post {
                    val ctx = appContext ?: return@post
                    launchMainActivityNow(ctx, pendingTab)
                }
            }

            appContext?.let {
                runCatching {
                    OverlayService.stop(it)
                }
            }
        } finally {
            isOverlayBeingRemoved = false
        }
    }

    fun removeOverlay() {
        triggerExitAnimation()
    }

    private fun sendAction(context: Context, action: String) {
        val intent = Intent(context, ClockActionReceiver::class.java).setAction(action)
        context.sendBroadcast(intent)
    }

    private class OverlayLifecycleOwner : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun performRestore(savedState: Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}
