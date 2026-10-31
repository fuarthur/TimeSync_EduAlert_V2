package com.ams.timesyncedualertv2.regression

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.ui.CourseDetailsActivity
import com.ams.timesyncedualertv2.ui.HomepageActivity
import com.ams.timesyncedualertv2.util.NotificationUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationEntryTest {
    private fun course()=CourseEntity(1,listOf(1),"09:00","10:00","Math","Hall","",-65536)
    @Test fun notificationRoutesThroughHomeForUndoResult() {
        val app=ApplicationProvider.getApplicationContext<TimeSyncApplication>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationUtils.postReminder(app,course())
        val notification=app.getSystemService(NotificationManager::class.java).activeNotifications.single().notification
        val intent=shadowOf(notification.contentIntent).savedIntent
        assertEquals(HomepageActivity::class.java.name,intent.component?.className)
        assertEquals(1,intent.getIntExtra("open_course_id",-1))
    }
    @Test fun homeOpensRequestedDetailsThroughResultLauncher() {
        val app=ApplicationProvider.getApplicationContext<TimeSyncApplication>()
        runBlocking { app.repository.save(course().copy(id=0)) }
        val c=Robolectric.buildActivity(HomepageActivity::class.java,Intent(app,HomepageActivity::class.java).putExtra("open_course_id",1)).setup()
        var launched:Intent?=null
        for(i in 0..300) {
            shadowOf(Looper.getMainLooper()).idle()
            launched=shadowOf(c.get()).nextStartedActivityForResult?.intent
            if(launched!=null) break
            Thread.sleep(10)
        }
        assertNotNull("Notification details must have a result caller for Undo",launched)
        assertEquals(CourseDetailsActivity::class.java.name,launched?.component?.className)
        c.pause().stop().destroy()
    }
}
