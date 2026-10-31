package com.ams.timesyncedualertv2

import android.app.Application
import androidx.room.Room
import com.ams.timesyncedualertv2.db.AppDatabase

class TimeSyncApplication : Application() {
    val repository by lazy { com.ams.timesyncedualertv2.data.CourseRepository(database) }
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "courses.db").build()
    }
}
