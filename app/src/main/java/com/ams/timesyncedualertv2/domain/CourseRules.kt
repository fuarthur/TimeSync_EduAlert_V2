package com.ams.timesyncedualertv2.domain

import com.ams.timesyncedualertv2.model.CourseEntity
import java.time.LocalTime

object CourseRules {
    fun time(value: String): LocalTime {
        require(value.matches(Regex("[0-9]{2}:[0-9]{2}"))) { "Use a time in HH:mm format" }
        return try { LocalTime.parse(value) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid time: $value", e)
        }
    }

    fun validate(course: CourseEntity): CourseEntity {
        require(course.id >= 0) { "Invalid course ID" }
        require(course.weekday.isNotEmpty() && course.weekday.all { it in 1..7 }) { "Select at least one valid weekday" }
        require(course.weekday.distinct().size == course.weekday.size) { "Duplicate weekdays" }
        require(time(course.endTime).isAfter(time(course.startTime))) { "End time must be later than start time" }
        require(course.name.isNotBlank() && course.name.length <= 120) { "Enter a course name (up to 120 characters)" }
        require(course.location.length <= 160 && course.description.length <= 4000) { "Course details are too long" }
        return course.copy(weekday=course.weekday.sorted(), name=course.name.trim(), location=course.location.trim(), description=course.description.trim())
    }

    fun validateSchedule(courses:List<CourseEntity>):List<CourseEntity> {
        val valid=courses.map(::validate)
        for(day in 1..7) {
            val ordered=valid.filter { day in it.weekday }.sortedBy { it.startTime }
            ordered.zipWithNext().forEach { (first,second) ->
                require(first.endTime<=second.startTime) { "Overlapping courses: ${first.name} and ${second.name}" }
            }
        }
        return valid
    }

    fun overlaps(first: CourseEntity, second: CourseEntity): Boolean =
        first.weekday.any { it in second.weekday } &&
            time(first.startTime) < time(second.endTime) && time(first.endTime) > time(second.startTime)
}
