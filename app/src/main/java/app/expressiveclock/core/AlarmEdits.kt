package app.expressiveclock.core

/** Label/audio changes must not undo a dismissed occurrence. Re-arming/time changes do. */
internal fun Alarm.preservingDismissal(previous: Alarm?): Alarm {
    val sameOccurrence = previous != null && previous.enabled == enabled &&
        previous.time == time && previous.days == days && previous.specificDate == specificDate
    return copy(snoozedUntilMillis = null,
        skippedOccurrenceMillis = if (sameOccurrence) previous?.skippedOccurrenceMillis else null,
        skippedOccurrenceDate = if (sameOccurrence) previous?.skippedOccurrenceDate else null)
}
