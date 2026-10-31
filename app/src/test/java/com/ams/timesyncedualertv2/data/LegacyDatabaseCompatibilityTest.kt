package com.ams.timesyncedualertv2.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegacyDatabaseCompatibilityTest {
    @Test
    fun originalVersionOneDatabaseKeepsCoursesAndAcceptsNewWrites() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "legacy-course-v1.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        // Schema and identity from the original Room 2.6 export, before the toolchain upgrade.
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL(
                """CREATE TABLE courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    weekday TEXT NOT NULL, startTime TEXT NOT NULL, endTime TEXT NOT NULL,
                    name TEXT NOT NULL, location TEXT NOT NULL, description TEXT NOT NULL,
                    color INTEGER NOT NULL)""",
            )
            old.execSQL(
                "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            old.execSQL(
                "INSERT INTO room_master_table VALUES (42, '39ac5a2ae51cb3b9219ac22c2c748728')",
            )
            old.execSQL(
                "INSERT INTO courses VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(7, "1,6", "09:00", "10:00", "旧课程", "Hall", "Notes\n笔记", -65536),
            )
            old.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val repository = CourseRepository(database)
            val original =
                CourseEntity(7, listOf(1, 6), "09:00", "10:00", "旧课程", "Hall", "Notes\n笔记", -65536)
            assertEquals(listOf(original), repository.all())
            val added = repository.save(original.copy(id = 0, weekday = listOf(2), name = "New"))
            assertTrue(added > 0)
            assertEquals(original, repository.get(7))
            assertEquals(2, repository.all().size)
        } finally {
            database.close()
        }
    }
}
