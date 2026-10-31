package com.ams.timesyncedualertv2.regression

import android.widget.Button
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.ui.CourseActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DraftStateTest {
    @Test fun recreationKeepsChosenTimeAndColor() {
        val c=Robolectric.buildActivity(CourseActivity::class.java).setup()
        val a=c.get()
        for ((name,value) in mapOf("startTime" to "09:00","endTime" to "10:00","selectedColor" to -16776961))
            CourseActivity::class.java.getDeclaredField(name).apply { isAccessible=true; set(a,value) }
        a.findViewById<Button>(R.id.button_start_time).text="09:00"
        c.recreate()
        assertEquals("09:00",c.get().findViewById<Button>(R.id.button_start_time).text.toString())
        val color=CourseActivity::class.java.getDeclaredField("selectedColor").apply { isAccessible=true }.get(c.get())
        assertEquals(-16776961,color)
        c.pause().stop().destroy()
    }
}
