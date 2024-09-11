package com.ams.timesyncedualertv2.db

import androidx.room.*
import com.ams.timesyncedualertv2.model.CourseEntity

@Dao
interface CourseDao {
    @Insert
    suspend fun insert(course: CourseEntity)

    @Query("SELECT * FROM courses WHERE weekday LIKE '%' || :weekday || '%'")
    suspend fun getCoursesForWeekday(weekday: Int): List<CourseEntity>

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}
