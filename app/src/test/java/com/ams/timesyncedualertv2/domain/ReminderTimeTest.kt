package com.ams.timesyncedualertv2.domain

import com.ams.timesyncedualertv2.model.CourseEntity
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ReminderTimeTest {
    private fun course(time: String = "09:00") =
        CourseEntity(1, listOf(1, 5), time, "10:00", "Math", "Hall", "", -65536)

    @Test
    fun nextMondayUsesAdvanceMinutes() {
        assertEquals(
            "2026-11-02T08:50Z[UTC]",
            ReminderTime.next(course(), 1, 10, ZonedDateTime.parse("2026-10-30T09:00Z[UTC]"))
                .toString(),
        )
    }

    @Test
    fun elapsedReminderMovesToNextWeek() {
        assertEquals(
            "2026-11-06T08:50Z[UTC]",
            ReminderTime.next(course(), 5, 10, ZonedDateTime.parse("2026-10-30T09:00Z[UTC]"))
                .toString(),
        )
    }

    @Test
    fun reminderCanFallOnPreviousDay() {
        assertEquals(
            "2026-11-01T23:55Z[UTC]",
            ReminderTime.next(course("00:05"), 1, 10, ZonedDateTime.parse("2026-10-31T12:00Z[UTC]"))
                .toString(),
        )
    }

    @Test
    fun springDstKeepsLocalClassTime() {
        assertEquals(
            "2026-03-09T08:50-04:00[America/New_York]",
            ReminderTime.next(
                    course(),
                    1,
                    10,
                    ZonedDateTime.parse("2026-03-07T12:00-05:00[America/New_York]"),
                )
                .toString(),
        )
    }
}
