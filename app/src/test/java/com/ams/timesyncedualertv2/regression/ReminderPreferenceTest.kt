package com.ams.timesyncedualertv2.regression

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.NotificationUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReminderPreferenceTest {
    @Test fun disabledRemindersRemainUnscheduledAfterRefresh() = runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>()
        shadowOf(c as android.app.Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val db=Room.databaseBuilder(c,AppDatabase::class.java,"courses.db").build()
        db.courseDao().insert(CourseEntity(1,listOf(1,5),"09:00","10:00","Math","Hall","",-65536))
        c.getSharedPreferences("user_settings",0).edit().putBoolean("enable_notifications",false).commit()
        NotificationUtils.reschedule(c)
        assertEquals(0,shadowOf(c.getSystemService(AlarmManager::class.java)).scheduledAlarms.size)
        db.close()
    }
}
