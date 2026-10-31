package com.ams.timesyncedualertv2.db

import androidx.room.*
import com.ams.timesyncedualertv2.model.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY startTime, id")
    fun observeAll(): Flow<List<CourseEntity>>

    @Insert suspend fun insert(course: CourseEntity): Long

    @Insert suspend fun insertAll(courses: List<CourseEntity>)

    @Update suspend fun update(course: CourseEntity)

    @Delete suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses") suspend fun deleteAllCourses()

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: Int): CourseEntity?

    @Query("SELECT * FROM courses ORDER BY startTime, id")
    suspend fun getAllCourses(): List<CourseEntity>
}
