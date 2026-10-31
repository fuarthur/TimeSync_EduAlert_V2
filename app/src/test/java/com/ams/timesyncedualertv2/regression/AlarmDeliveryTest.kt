package com.ams.timesyncedualertv2.regression

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.NotificationUtils
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class AlarmDeliveryTest {
    private fun app()=ApplicationProvider.getApplicationContext<TimeSyncApplication>().also {
        shadowOf(it).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        it.getSharedPreferences("user_settings",0).edit().putBoolean("enable_notifications",true).putInt("reminder_time",10).commit()
    }
    @Test fun staleDeliveryCannotOverwriteEditedCourseAlarm()=runBlocking {
        val app=app()
        val old=CourseEntity(weekday=listOf(1),startTime="09:00",endTime="10:00",name="Math",location="Hall",description="",color=-65536)
        val id=app.repository.save(old)
        NotificationUtils.reschedule(app)
        val manager=shadowOf(app.getSystemService(AlarmManager::class.java))
        val oldTrigger=manager.scheduledAlarms.single().triggerAtMs
        app.repository.save(old.copy(id=id,startTime="07:00",endTime="08:00"))
        NotificationUtils.reschedule(app)
        val editedTrigger=manager.scheduledAlarms.single().triggerAtMs
        assertNotEquals(oldTrigger,editedTrigger)
        NotificationUtils.handleAlarm(app,id,1,oldTrigger)
        assertEquals(editedTrigger,manager.scheduledAlarms.single().triggerAtMs)
        assertEquals(0,app.getSystemService(NotificationManager::class.java).activeNotifications.size)
    }
    @Test fun currentDeliveryPostsAndRearmsInTheFuture()=runBlocking {
        val app=app()
        val id=app.repository.save(CourseEntity(weekday=listOf(1),startTime="09:00",endTime="10:00",name="Math",location="Hall",description="",color=-65536))
        NotificationUtils.reschedule(app)
        val manager=shadowOf(app.getSystemService(AlarmManager::class.java))
        val trigger=manager.scheduledAlarms.single().triggerAtMs
        NotificationUtils.handleAlarm(app,id,1,trigger,Instant.ofEpochMilli(trigger+1000).atZone(ZoneId.systemDefault()))
        assertEquals(1,app.getSystemService(NotificationManager::class.java).activeNotifications.size)
        assertTrue(manager.scheduledAlarms.single().triggerAtMs>trigger)
    }
}
