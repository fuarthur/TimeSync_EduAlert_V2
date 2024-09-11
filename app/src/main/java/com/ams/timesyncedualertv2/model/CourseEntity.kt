package com.ams.timesyncedualertv2.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekday: List<Int>,
    val startTime: String,
    val endTime: String,
    val name: String,
    val location: String,
    val description: String,
    val color: Int
)
