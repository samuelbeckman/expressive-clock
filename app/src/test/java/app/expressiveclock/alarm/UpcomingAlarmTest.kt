package app.expressiveclock.alarm

import app.expressiveclock.core.Alarm
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class UpcomingAlarmTest {
    private val now = ZonedDateTime.of(2026,11,16,6,30,0,0,ZoneId.systemDefault())
    private val alarm = Alarm(42,LocalTime.of(7,0), days=DayOfWeek.entries.toSet())
    @Test fun skipPreservesRepeatAndMovesToTomorrow() {
        val at=alarm.nextFire(now)!!.toInstant().toEpochMilli()
        val skipped=alarm.copy(skippedOccurrenceMillis=at)
        assertEquals(alarm.days, skipped.days)
        assertEquals(now.toLocalDate().plusDays(1),skipped.nextFire(now)!!.toLocalDate())
        assertFalse(UpcomingAlarm.matches(skipped,at))
    }
    @Test fun editedOrDisabledAlarmRejectsOldCallback() {
        val at=alarm.nextFire(now)!!.toInstant().toEpochMilli()
        assertTrue(UpcomingAlarm.matches(alarm,at))
        assertFalse(UpcomingAlarm.matches(alarm.copy(time=LocalTime.of(8,0)),at))
        assertFalse(UpcomingAlarm.matches(alarm.copy(enabled=false),at))
    }
    @Test fun snoozeMatchesOnlySnoozedOccurrence() {
        val at=now.plusMinutes(5).toInstant().toEpochMilli()
        val snoozed=alarm.copy(snoozedUntilMillis=at)
        assertTrue(UpcomingAlarm.matches(snoozed,at))
        assertFalse(UpcomingAlarm.matches(snoozed,at+60_000))
    }
    @Test fun oneShotSkippedOccurrenceDoesNotRollToTomorrow() {
        val one=alarm.copy(days=emptySet()); val at=one.nextFire(now)!!.toInstant().toEpochMilli()
        assertNull(one.copy(skippedOccurrenceMillis=at).nextFire(now))
    }
    @Test fun unrelatedOldSkipDoesNotSuppressFutureAlarm() {
        val at=alarm.nextFire(now)!!.toInstant().toEpochMilli()
        assertEquals(alarm.nextFire(now),alarm.copy(skippedOccurrenceMillis=at-86400000).nextFire(now))
    }
    @Test fun skippedLocalDateSurvivesTimezoneRescheduling() {
        val skipped = alarm.copy(skippedOccurrenceDate = now.toLocalDate())
        val changed = now.withZoneSameLocal(ZoneId.of("America/Los_Angeles"))
        assertEquals(now.toLocalDate().plusDays(1), skipped.nextFire(changed)!!.toLocalDate())
    }
    @Test fun weeklySkipPreservesNextWeekAcrossDaylightSaving() {
        val before = ZonedDateTime.of(2026,3,8,1,0,0,0,ZoneId.of("America/New_York"))
        val weekly = alarm.copy(time=LocalTime.of(3,30),days=setOf(DayOfWeek.SUNDAY),skippedOccurrenceDate=before.toLocalDate())
        assertEquals(LocalDate.of(2026,3,15),weekly.nextFire(before)!!.toLocalDate())
        assertEquals(LocalTime.of(3,30),weekly.nextFire(before)!!.toLocalTime())
    }

    @Test fun specificDateFiresOnTargetCalendarDate() {
        val targetDate = now.toLocalDate().plusDays(14)
        val dateAlarm = alarm.copy(days = emptySet(), specificDate = targetDate)
        assertTrue(dateAlarm.isOneShot)
        val next = dateAlarm.nextFire(now)
        assertNotNull(next)
        assertEquals(targetDate, next!!.toLocalDate())
        assertEquals(LocalTime.of(7, 0), next.toLocalTime())
    }

    @Test fun pastSpecificDateDoesNotFire() {
        val pastDate = now.toLocalDate().minusDays(1)
        val dateAlarm = alarm.copy(days = emptySet(), specificDate = pastDate)
        assertNull(dateAlarm.nextFire(now))
    }

    @Test fun specificDateSkippedOccurrenceDoesNotFire() {
        val targetDate = now.toLocalDate().plusDays(3)
        val dateAlarm = alarm.copy(days = emptySet(), specificDate = targetDate)
        val at = dateAlarm.nextFire(now)!!.toInstant().toEpochMilli()
        assertNull(dateAlarm.copy(skippedOccurrenceMillis = at).nextFire(now))
        assertNull(dateAlarm.copy(skippedOccurrenceDate = targetDate).nextFire(now))
    }

    @Test fun specificDateRepeatLabelFormatting() {
        val today = now.toLocalDate()
        assertEquals("Today", alarm.copy(specificDate = today).repeatLabel(today))
        assertEquals("Tomorrow", alarm.copy(specificDate = today.plusDays(1)).repeatLabel(today))
    }
}
