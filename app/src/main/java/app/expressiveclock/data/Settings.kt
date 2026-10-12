package app.expressiveclock.data

import app.expressiveclock.alarm.Notifications
import app.expressiveclock.ui.theme.Palette
import java.time.DayOfWeek

/**
 * Every preference the app has, in one tree.
 *
 * Grouped by feature, then surfaced from one persistent app-wide settings sheet. This keeps the
 * navigation affordance consistent while leaving room to add more preferences later.
 */
data class ClockSettings(
    val alarms: AlarmSettings = AlarmSettings(),
    val world: WorldClockSettings = WorldClockSettings(),
    val timers: TimerSettings = TimerSettings(),
    val theme: ThemeSettings = ThemeSettings(),
    val defaultStartupPage: DefaultStartupPage = DefaultStartupPage.ALARMS,
    val hasCompletedInitialSetup: Boolean = false,
)

data class AlarmSettings(
    /** How long a ringing alarm keeps ringing before it gives up. */
    /** Minutes a ringing alarm keeps going before it gives up. **0 means never.** */
    val silenceAfterMinutes: Int = 10,
    val snoozeMinutes: Int = 10,
    val weekStart: WeekStart = WeekStart.SYSTEM,
)

data class WorldClockSettings(
    val showSeconds: Boolean = false,
    val hourFormat: HourFormat = HourFormat.SYSTEM,
)

data class TimerSettings(
    /** A `content://` ringtone URI, or null for the system default alarm sound. */
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    /** Presets are stored as total seconds, in the order shown on the timer page. */
    val presets: List<Int> = DEFAULT_TIMER_PRESETS,
)

val DEFAULT_TIMER_PRESETS: List<Int> = listOf(60, 120, 300, 600)
const val SILENT = "silent"

fun formatPresetLabel(seconds: Int): String = when {
    seconds >= 3600 && seconds % 3600 == 0 -> "${seconds / 3600}h"
    seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    seconds >= 60 && seconds % 60 == 0 -> "${seconds / 60} min"
    seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds}s"
}

const val FACTORY_OVERLAY_OFFSET_X = 0
const val FACTORY_OVERLAY_OFFSET_Y = 9
const val FACTORY_OVERLAY_WIDTH_OFFSET = -43
const val FACTORY_OVERLAY_HEIGHT_OFFSET = 1

data class ThemeSettings(
    /**
     * Wallpaper colour is the default, per the platform's own guidance: a clock is a background
     * app and should look like it belongs to the phone rather than to itself. [palette] is what it
     * falls back to when dynamic colour is off, or unavailable below API 31.
     */
    val dynamicColor: Boolean = true,
    val palette: Palette = Palette.CONCEPT,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val dynamicIsland: Boolean = false,
    val overlayOffsetX: Int = FACTORY_OVERLAY_OFFSET_X,
    val overlayOffsetY: Int = FACTORY_OVERLAY_OFFSET_Y,
    val overlayWidthOffset: Int = FACTORY_OVERLAY_WIDTH_OFFSET,
    val overlayHeightOffset: Int = FACTORY_OVERLAY_HEIGHT_OFFSET,
)

enum class DarkMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class HourFormat(val label: String) { SYSTEM("System default"), H12("12-hour"), H24("24-hour") }

enum class WeekStart(val label: String, val day: DayOfWeek?) {
    SYSTEM("System default", null),
    SATURDAY("Saturday", DayOfWeek.SATURDAY),
    SUNDAY("Sunday", DayOfWeek.SUNDAY),
    MONDAY("Monday", DayOfWeek.MONDAY),
}

enum class DefaultStartupPage(val label: String, val tabKey: String) {
    ALARMS("Alarms", Notifications.TAB_ALARMS),
    TIMER("Timer", Notifications.TAB_TIMERS),
    STOPWATCH("Stopwatch", Notifications.TAB_STOPWATCH);

    companion object {
        fun fromName(name: String?): DefaultStartupPage {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ALARMS
        }
    }
}

/**
 * The seven weekday letters, rotated to start on [start].
 *
 * The alarm tile draws these, so the setting has to reach all the way into the grid rather than
 * only into a picker. An app that starts the week on Monday in the editor and on Sunday in the
 * tile is worse than one that never offered the choice.
 */
fun WeekStart.order(systemFirstDay: DayOfWeek): List<DayOfWeek> {
    val first = day ?: systemFirstDay
    return (0..6).map { DayOfWeek.of((first.value - 1 + it) % 7 + 1) }
}
