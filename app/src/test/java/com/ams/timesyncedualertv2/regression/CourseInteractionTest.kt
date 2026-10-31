package com.ams.timesyncedualertv2.regression

import android.widget.FrameLayout
import com.ams.timesyncedualertv2.model.CourseAdapter
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.ui.HomepageActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CourseInteractionTest {
    @Test fun oneTapOpensCourseDetails() {
        val controller=Robolectric.buildActivity(HomepageActivity::class.java).setup()
        val activity=controller.get()
        val adapter=CourseAdapter(activity,listOf(CourseEntity(1,listOf(1),"09:00","10:00","Math","Hall","",-65536)))
        val holder=adapter.onCreateViewHolder(FrameLayout(activity),0)
        adapter.onBindViewHolder(holder,0)
        holder.itemView.performClick()
        val intent=shadowOf(activity).nextStartedActivity
        assertNotNull("A single tap must open details",intent)
        assertEquals(1,intent.getIntExtra("course_id",-1))
        controller.pause().stop().destroy()
    }
}
