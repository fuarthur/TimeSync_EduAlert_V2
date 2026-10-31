package com.ams.timesyncedualertv2.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.Converters

@Database(entities = [CourseEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        fun getInstance(context: android.content.Context): AppDatabase =
            (context.applicationContext as com.ams.timesyncedualertv2.TimeSyncApplication).database
    }
    abstract fun courseDao(): CourseDao
}
