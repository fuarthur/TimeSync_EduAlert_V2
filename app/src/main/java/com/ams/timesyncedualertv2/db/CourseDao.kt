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

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: Int): CourseEntity?

    @Query(
        """
    UPDATE courses 
    SET weekday = :weekday, 
        startTime = :startTime, 
        endTime = :endTime, 
        name = :name, 
        location = :location, 
        description = :description, 
        color = :color 
    WHERE id = :id
    """
    )
    suspend fun updateCourse(
        id: Int,
        weekday: List<Int>,
        startTime: String,
        endTime: String,
        name: String,
        location: String,
        description: String,
        color: Int
    )
}
