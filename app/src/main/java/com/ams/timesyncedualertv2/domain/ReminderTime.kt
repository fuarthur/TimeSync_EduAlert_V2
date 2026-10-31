package com.ams.timesyncedualertv2.domain

import com.ams.timesyncedualertv2.model.CourseEntity
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object ReminderTime {
    fun next(course: CourseEntity, weekday: Int, minutes: Int, now: ZonedDateTime): ZonedDateTime {
        require(weekday in 1..7 && minutes in 1..1440)
        val time=CourseRules.time(course.startTime)
        var date=now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(weekday)))
        var trigger=date.atTime(time).atZone(now.zone).minusMinutes(minutes.toLong())
        if (!trigger.isAfter(now)) {
            date=date.plusWeeks(1)
            trigger=date.atTime(time).atZone(now.zone).minusMinutes(minutes.toLong())
        }
        return trigger
    }
}
