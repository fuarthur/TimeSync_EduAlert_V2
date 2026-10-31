package com.ams.timesyncedualertv2.regression

import android.content.Intent
import android.os.Looper
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.ui.CourseActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DuplicateCourseTest {
    @Test
    fun duplicateOpensPrefilledDraftWithoutAddingARecord() {
        val app = ApplicationProvider.getApplicationContext<TimeSyncApplication>()
        val id = runBlocking {
            app.repository.save(
                CourseEntity(
                    weekday = listOf(1),
                    startTime = "09:00",
                    endTime = "10:00",
                    name = "Math",
                    location = "Hall",
                    description = "Notes",
                    color = -65536,
                ),
            )
        }
        val c =
            Robolectric.buildActivity(
                    CourseActivity::class.java,
                    Intent(app, CourseActivity::class.java).putExtra("duplicate_id", id),
                )
                .setup()
        for (i in 0..300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (c.get().findViewById<EditText>(R.id.edittext_course_name).text.isNotEmpty()) break
            Thread.sleep(10)
        }
        assertEquals(
            "Math (copy)",
            c.get().findViewById<EditText>(R.id.edittext_course_name).text.toString(),
        )
        assertEquals(
            "Notes",
            c.get().findViewById<EditText>(R.id.edittext_course_description).text.toString(),
        )
        assertEquals(1, runBlocking { app.repository.all().size })
        c.pause().stop().destroy()
    }
}
