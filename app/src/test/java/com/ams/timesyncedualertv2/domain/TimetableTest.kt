package com.ams.timesyncedualertv2.domain

import com.ams.timesyncedualertv2.model.CourseEntity
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class TimetableTest {
    private fun course(id:Int,day:Int,start:String,end:String,name:String="Math",room:String="Hall")=CourseEntity(id,listOf(day),start,end,name,room,"",-65536)
    @Test fun ongoingCourseWinsOverLaterClass() {
        val next=Timetable.next(listOf(course(2,5,"11:00","12:00"),course(1,5,"09:00","10:00")),ZonedDateTime.parse("2026-10-30T09:30Z[UTC]"))!!
        assertEquals(1,next.course.id); assertTrue(next.ongoing)
    }
    @Test fun weekendCourseIsFoundBeforeMonday() {
        assertEquals(2,Timetable.next(listOf(course(1,1,"09:00","10:00"),course(2,6,"09:00","10:00")),ZonedDateTime.parse("2026-10-30T12:00Z[UTC]"))!!.course.id)
    }
    @Test fun finishedCourseMovesToNextWeek() {
        assertEquals("2026-11-06T09:00Z[UTC]",Timetable.next(listOf(course(1,5,"09:00","10:00")),ZonedDateTime.parse("2026-10-30T10:00Z[UTC]"))!!.start.toString())
    }
    @Test fun searchMatchesNameOrRoomAndKeepsDayFilter() {
        val courses=listOf(course(1,6,"09:00","10:00","Algorithms","Siebel"),course(2,1,"09:00","10:00","Algorithms","Siebel"))
        assertEquals(listOf(1),Timetable.onDay(courses,6," sieBEL ").map { it.id })
        assertTrue(Timetable.onDay(courses,6,"physics").isEmpty())
    }
}
