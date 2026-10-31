package com.ams.timesyncedualertv2.regression

import android.content.Context
import android.os.Looper
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.ui.CourseActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class CourseEditorTest {
    @Test fun submitRejectsZeroLengthCourse() {
        val controller=Robolectric.buildActivity(CourseActivity::class.java).setup()
        val a=controller.get()
        a.findViewById<CheckBox>(R.id.checkbox_monday).isChecked=true
        for (id in listOf(R.id.edittext_course_name,R.id.edittext_course_location,R.id.edittext_course_description)) a.findViewById<EditText>(id).setText("Demo")
        // This is the exact state produced by selecting only a start time.
        for (name in listOf("startTime","endTime")) CourseActivity::class.java.getDeclaredField(name).apply { isAccessible=true; set(a,"09:00") }
        a.findViewById<Button>(R.id.button_submit).performClick()
        for (attempt in 0..300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (ShadowToast.getTextOfLatestToast() != null) break
            Thread.sleep(10)
        }
        val db=Room.databaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java,"courses.db").build()
        assertEquals(0,runBlocking { db.courseDao().getAllCourses().size })
        controller.pause().stop().destroy(); db.close()
    }
}
