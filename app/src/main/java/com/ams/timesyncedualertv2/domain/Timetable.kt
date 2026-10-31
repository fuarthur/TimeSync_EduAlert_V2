package com.ams.timesyncedualertv2.domain

import com.ams.timesyncedualertv2.model.CourseEntity
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

data class CourseOccurrence(val course:CourseEntity,val start:ZonedDateTime,val ongoing:Boolean)

object Timetable {
    fun onDay(courses:List<CourseEntity>,day:Int,query:String=""):List<CourseEntity> {
        val text=query.trim()
        return courses.filter { day in it.weekday && (text.isEmpty() || it.name.contains(text,true) || it.location.contains(text,true)) }
            .sortedWith(compareBy<CourseEntity> { it.startTime }.thenBy { it.id })
    }
    fun next(courses:List<CourseEntity>,now:ZonedDateTime):CourseOccurrence? = courses.flatMap { c ->
        if(runCatching { CourseRules.validate(c) }.isFailure) return@flatMap emptyList()
        val time=CourseRules.time(c.startTime)
        val endTime=CourseRules.time(c.endTime)
        c.weekday.map { day ->
            var date=now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(day)))
            var start=date.atTime(time).atZone(now.zone)
            val end=date.atTime(endTime).atZone(now.zone)
            val ongoing=!now.isBefore(start) && now.isBefore(end)
            if(!ongoing && !start.isAfter(now)) { date=date.plusWeeks(1); start=date.atTime(time).atZone(now.zone) }
            CourseOccurrence(c,start,ongoing)
        }
    }.minWithOrNull(compareBy<CourseOccurrence> { !it.ongoing }.thenBy { it.start })
}
