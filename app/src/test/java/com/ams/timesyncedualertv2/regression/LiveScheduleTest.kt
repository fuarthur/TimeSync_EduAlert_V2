package com.ams.timesyncedualertv2.regression

import android.content.Context
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.ui.HomepageActivity
import com.ams.timesyncedualertv2.ui.WeekdayFragment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LiveScheduleTest {
    @Test fun visibleListUpdatesWhenDatabaseIsClearedElsewhere() {
        val db=AppDatabase.getInstance(ApplicationProvider.getApplicationContext())
        runBlocking { db.courseDao().insert(CourseEntity(1,listOf(1),"09:00","10:00","Math","Hall","",-65536)) }
        val controller=Robolectric.buildActivity(HomepageActivity::class.java).setup()
        val activity=controller.get()
        activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.pager).setCurrentItem(0,false)
        fun list():RecyclerView?=activity.supportFragmentManager.fragments.filterIsInstance<WeekdayFragment>().firstOrNull { it.arguments?.getString("weekday")=="Mon." }?.view?.findViewById(R.id.courses_recycler_view)
        for (i in 0..300) { shadowOf(Looper.getMainLooper()).idle(); if(list()?.adapter?.itemCount==1) break; Thread.sleep(10) }
        assertEquals(1,list()?.adapter?.itemCount)
        runBlocking { db.courseDao().deleteAllCourses() }
        for (i in 0..100) { shadowOf(Looper.getMainLooper()).idle(); if(list()?.adapter?.itemCount==0) break; Thread.sleep(10) }
        assertEquals("Visible rows must follow database changes",0,list()?.adapter?.itemCount)
        controller.pause().stop().destroy(); db.close()
    }
}
