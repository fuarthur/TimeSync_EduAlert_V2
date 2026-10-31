package com.ams.timesyncedualertv2.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UndoCourseTest {
    @Test
    fun deletedCourseCanBeRestoredWithItsDetails() = runBlocking {
        val db =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext<Context>(),
                    AppDatabase::class.java,
                )
                .build()
        try {
            val repo = CourseRepository(db)
            val id =
                repo.save(
                    CourseEntity(
                        weekday = listOf(6),
                        startTime = "09:00",
                        endTime = "10:00",
                        name = "Art",
                        location = "Studio",
                        description = "Bring sketchbook",
                        color = -65536,
                    ),
                )
            val deleted = repo.delete(id)!!
            assertTrue(repo.all().isEmpty())
            repo.restore(deleted)
            assertEquals(deleted, repo.all().single())
        } finally {
            db.close()
        }
    }
}
