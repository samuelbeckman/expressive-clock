package app.expressiveclock.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import app.expressiveclock.ui.screens.SelectionToolbar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expressiveclock.alarm.Notifications
import app.expressiveclock.core.Alarm
import app.expressiveclock.data.DefaultStartupPage
import app.expressiveclock.ui.screens.AlarmsScreen
import app.expressiveclock.ui.screens.OnboardingScreen
import app.expressiveclock.ui.screens.StopwatchScreen
import app.expressiveclock.ui.screens.SettingsScreen
import app.expressiveclock.ui.screens.TimersScreen
import app.expressiveclock.ui.sheets.AlarmEditSheet
import app.expressiveclock.ui.sheets.NewAlarmPickerSheet
import app.expressiveclock.ui.theme.ClockTheme

/** Breathing room between the dock and the navigation bar. */
private val DOCK_CLEARANCE = 20.dp

/**
 * How much of the bottom of every screen the floating dock is standing on.
 *
 * Derived from [DOCK_HEIGHT] rather than restated as a number. It was written out as a literal 96
 * once ("64 of pill, 20 under it, 12 above the last row"), and the moment the pill grew to 72 the
 * comment was still true and the constant was silently 8 dp short, which shows up as the last row
 * of every list tucked a little too far under the dock. There is nothing to notice and nothing to
 * fail; it just quietly looks wrong.
 */
private val DOCK_RESERVE = DOCK_HEIGHT + DOCK_CLEARANCE + 12.dp

enum class Tab(val label: String, val icon: ImageVector, val key: String) {
    ALARMS("Alarms", Icons.Outlined.Alarm, Notifications.TAB_ALARMS),
    TIMERS("Timers", Icons.Outlined.HourglassEmpty, Notifications.TAB_TIMERS),
    STOPWATCH("Stopwatch", Icons.Outlined.Timer, Notifications.TAB_STOPWATCH),
}

/**
 * The app shell.
 *
 * Navigation is [ClockDock], a floating pill with a detached add button and no navigation bar, so
 * the alarm grid and the world-clock list run full-bleed to the bottom edge. That component owes
 * the accessibility contract a `ShortNavigationBar` would have supplied for free; see its own
 * documentation for what it pays and why.
 *
 * Settings are always available from the top-right gear, regardless of which page is visible.
 */
@Composable
fun ClockApp(startTab: String? = null, vm: ClockViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val isSettingsLoaded by vm.isSettingsLoaded.collectAsStateWithLifecycle()

    ClockTheme(settings.theme) {
        val defaultTab = when (settings.defaultStartupPage) {
            DefaultStartupPage.ALARMS -> Tab.ALARMS
            DefaultStartupPage.TIMER -> Tab.TIMERS
            DefaultStartupPage.STOPWATCH -> Tab.STOPWATCH
        }

        var tab by rememberSaveable { mutableStateOf<Tab?>(null) }

        // Hydrate startup tab once settings load from DataStore on cold start
        LaunchedEffect(isSettingsLoaded, startTab) {
            if (isSettingsLoaded && tab == null) {
                tab = startTab?.let { k -> Tab.entries.firstOrNull { it.key == k } } ?: defaultTab
            } else if (startTab != null) {
                // Explicit navigation (Dynamic Island / Notification tap)
                startTab.let { k -> Tab.entries.firstOrNull { it.key == k } }?.let { tab = it }
            }
        }

        val activeTab = tab ?: startTab?.let { k -> Tab.entries.firstOrNull { it.key == k } } ?: defaultTab

        var editing by remember { mutableStateOf<Alarm?>(null) }
        var showNewAlarmPicker by remember { mutableStateOf(false) }
        var alarmSelectionCount by remember { mutableIntStateOf(0) }
        var clearSelectionSignal by remember { mutableIntStateOf(0) }
        var deleteClickSignal by remember { mutableIntStateOf(0) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        val settingsProgress = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        fun openSettings() {
            showSettings = true
            scope.launch {
                settingsProgress.snapTo(0f)
                settingsProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                )
            }
        }

        fun closeSettings() {
            scope.launch {
                settingsProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                )
                showSettings = false
            }
        }

        PredictiveBackHandler(enabled = showSettings) { backEvents ->
            try {
                backEvents.collect { event ->
                    settingsProgress.snapTo(1f - event.progress)
                }
                settingsProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
                showSettings = false
            } catch (_: CancellationException) {
                settingsProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
            }
        }
        val snackbar = remember { SnackbarHostState() }

        // Asked once, on first composition. Denying it costs the notifications and nothing else.
        // The alarm still rings, because the ringer is a foreground service and audio, not a post.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }

        val showOnboarding = !settings.hasCompletedInitialSetup
        var isPostOnboardingStaggerToken by rememberSaveable { mutableIntStateOf(0) }

        val onboardingTransition = updateTransition(targetState = showOnboarding, label = "onboardingTransition")

        onboardingTransition.AnimatedContent(
            transitionSpec = {
                if (!targetState) {
                    slideInHorizontally(
                        animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f),
                        initialOffsetX = { width -> width },
                    ) + fadeIn(tween(220)) togetherWith
                        slideOutHorizontally(
                            animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f),
                            targetOffsetX = { width -> -width },
                        ) + fadeOut(tween(200))
                } else {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { isSetup ->
            if (isSetup) {
                OnboardingScreen(
                    onComplete = {
                        isPostOnboardingStaggerToken = 1
                        vm.updateSettings { it.copy(hasCompletedInitialSetup = true) }
                    },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = settingsProgress.value
                                translationX = -size.width * 0.12f * p
                                alpha = 1f - 0.06f * p
                            },
                    ) {
                Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (activeTab == Tab.ALARMS && alarmSelectionCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .height(52.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        SelectionToolbar(
                            selectedCount = alarmSelectionCount,
                            onClearSelection = { clearSelectionSignal++ },
                            onDeleteClick = { deleteClickSignal++ },
                        )
                    }
                } else {
                    TopAppBar(
                        title = {},
                        actions = {
                            IconButton(onClick = ::openSettings) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                }
            },
        ) { padding ->
            // Keep the bars' insets but let each screen own its horizontal margin, so a grid
            // and a full-bleed dial can differ without fighting the Scaffold.
            //
            // The dock floats over the content rather than displacing it, so the space it occupies
            // has to be reserved here by hand; otherwise the last alarm tile sits under the pill.
            val ld = LocalLayoutDirection.current
            val underDock = padding.calculateBottomPadding() + DOCK_RESERVE
            val body = PaddingValues(
                start = padding.calculateStartPadding(ld) + 16.dp,
                end = padding.calculateEndPadding(ld) + 16.dp,
                top = padding.calculateTopPadding(),
                bottom = underDock,
            )
            val edgeToEdge = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = underDock,
            )

          GlassBackdrop(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    slideInHorizontally(
                        initialOffsetX = { width -> if (forward) width else -width },
                    ) + fadeIn() togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { width -> if (forward) -width else width },
                        ) + fadeOut()
                },
                label = "tab",
            ) { current ->
                when (current) {
                    Tab.ALARMS -> {
                        val alarms by vm.alarms.collectAsStateWithLifecycle()
                        AlarmsScreen(
                            alarms = alarms,
                            weekStart = settings.alarms.weekStart,
                            staggerEntranceToken = isPostOnboardingStaggerToken,
                            onStaggerConsumed = { isPostOnboardingStaggerToken = 0 },
                            onToggle = vm::toggleAlarm,
                            onEdit = { editing = it },
                            onAddAlarm = { showNewAlarmPicker = true },
                            onDeleteAlarms = { vm.deleteAlarms(it) },
                            onSelectionCountChange = { count ->
                                alarmSelectionCount = count
                            },
                            clearSelectionSignal = clearSelectionSignal,
                            deleteClickSignal = deleteClickSignal,
                            contentPadding = body,
                        )
                    }

                    Tab.TIMERS -> {
                        val timer by vm.timer.collectAsStateWithLifecycle()
                        // Only tick while a timer exists; the setting screen has nothing moving.
                        val now by rememberElapsedTicker(active = timer != null)
                        TimersScreen(
                            timer = timer,
                            draft = vm.draftDuration,
                            nowElapsedMillis = now,
                            presets = settings.timers.presets,
                            onDigit = vm::pressDigit,
                            onBackspace = vm::backspace,
                            onResetDraft = vm::resetTimerDraft,
                            onPreset = vm::setTimerPreset,
                            onStart = { vm.startTimer() },
                            onPauseResume = { vm.pauseOrResumeTimer() },
                            onAddTen = { vm.addTenSeconds() },
                            onCancel = { vm.cancelTimer() },
                            contentPadding = edgeToEdge,
                        )
                    }

                    Tab.STOPWATCH -> {
                        val sw by vm.stopwatch.collectAsStateWithLifecycle()
                        val now by rememberElapsedTicker(active = sw.running)
                        StopwatchScreen(
                            stopwatch = sw,
                            nowElapsedMillis = now,
                            onToggle = { vm.toggleStopwatch() },
                            onLap = { vm.lap() },
                            onReset = { vm.resetStopwatch() },
                            contentPadding = edgeToEdge,
                        )
                    }
                }
            }

            ClockDock(
                destinations = listOf(Tab.ALARMS, Tab.TIMERS, Tab.STOPWATCH),
                selected = activeTab,
                onSelect = { tab = it },
                // Only alarms need the detached action button. Timers and Stopwatch create
                // their content directly from the screen, so the dock stays visually centered.
                showAdd = activeTab == Tab.ALARMS,
                addLabel = "Add alarm",
                onAdd = { showNewAlarmPicker = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = DOCK_CLEARANCE),
                enableGlow = false,
            )
          }
            }
            }

            if (showSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = settingsProgress.value
                            translationX = size.width * (1f - p)
                            alpha = 0.92f + 0.08f * p
                        },
                ) {
                    SettingsScreen(
                        settings = settings,
                        onChange = vm::updateSettings,
                        onBack = ::closeSettings,
                        onReopenOnboarding = {
                            closeSettings()
                            vm.updateSettings { it.copy(hasCompletedInitialSetup = false) }
                        },
                    )
                }
            }
        }
    }
}

        editing?.let { draft ->
            AlarmEditSheet(
                initial = draft,
                weekStart = settings.alarms.weekStart,
                onDismiss = { editing = null },
                onSave = { vm.saveAlarm(it); editing = null },
                onDelete = if (draft.id == 0L) null else ({ id -> vm.deleteAlarm(id) }),
            )
        }

        if (showNewAlarmPicker) {
            NewAlarmPickerSheet(
                initial = vm.blankAlarm(),
                weekStart = settings.alarms.weekStart,
                onDismiss = { showNewAlarmPicker = false },
                onSave = { alarm -> vm.saveAlarm(alarm); showNewAlarmPicker = false },
            )
        }
    }
}
