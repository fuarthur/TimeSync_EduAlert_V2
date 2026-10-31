package com.ams.timesyncedualertv2.regression

import android.content.Context
import android.os.Looper
import android.widget.Button
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.ui.SettingsActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import androidx.appcompat.app.AlertDialog
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class ImportSafetyTest {
    @Test fun openingImportPickerPreservesExistingSchedule() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "courses.db").build()
        runBlocking { db.courseDao().insert(CourseEntity(weekday=listOf(1), startTime="09:00", endTime="10:00", name="Math", location="Hall", description="", color=-65536)) }
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        val activity = controller.get()
        activity.findViewById<Button>(R.id.button_import_schedule).performClick()
        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(-1).performClick()
        repeat(30) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertEquals("Selecting or cancelling a file must never delete courses", 1, runBlocking { db.courseDao().getAllCourses().size })
        controller.pause().stop().destroy()
        db.close()
    }
}
