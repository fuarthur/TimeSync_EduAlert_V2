package com.ams.timesyncedualertv2.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourseRepositoryTest {
    private fun course(start:String="09:00",end:String="10:00")=CourseEntity(weekday=listOf(1),startTime=start,endTime=end,name="Math",location="Hall",description="",color=-65536)
    @Test fun concurrentConflictingSavesOnlyInsertOnce() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).build()
        try {
            val repo=CourseRepository(db)
            val results=coroutineScope { (1..2).map { async(Dispatchers.IO) { runCatching { repo.save(course()) } } }.awaitAll() }
            assertEquals(1,results.count { it.isSuccess })
            assertEquals(1,db.courseDao().getAllCourses().size)
        } finally { db.close() }
    }
    @Test fun adjacentCoursesDoNotConflict()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).build()
        try { val repo=CourseRepository(db); repo.save(course()); repo.save(course("10:00","11:00")); assertEquals(2,db.courseDao().getAllCourses().size) } finally { db.close() }
    }
    @Test fun failedReplacementRetainsOldCourses()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).build()
        try {
            val repo=CourseRepository(db); repo.save(course())
            assertTrue(runCatching { repo.replace(listOf(course().copy(weekday=emptyList()))) }.isFailure)
            assertEquals("Math",db.courseDao().getAllCourses().single().name)
        } finally { db.close() }
    }
}
