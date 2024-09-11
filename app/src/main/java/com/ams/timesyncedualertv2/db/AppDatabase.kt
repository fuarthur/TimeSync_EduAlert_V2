package com.ams.timesyncedualertv2.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.Converters

@Database(entities = [CourseEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
}
