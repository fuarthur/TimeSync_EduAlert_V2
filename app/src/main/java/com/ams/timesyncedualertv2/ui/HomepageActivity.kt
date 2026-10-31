package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.util.NotificationUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

class HomepageActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        Ui.setup(this)
        val toolbar=findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.subtitle=DateFormat.getDateInstance(DateFormat.FULL).format(Date())
        toolbar.menu.add(0,1,0,R.string.settings).apply { setIcon(R.drawable.ic_settings); icon?.setTint(com.google.android.material.color.MaterialColors.getColor(toolbar,com.google.android.material.R.attr.colorOnSurface)); setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS) }
        toolbar.setOnMenuItemClickListener { startActivity(Intent(this,SettingsActivity::class.java)); true }
        val pager=findViewById<ViewPager2>(R.id.pager)
        val adapter=ViewPagerAdapter(this)
        pager.adapter=adapter
        TabLayoutMediator(findViewById<TabLayout>(R.id.tab_layout),pager) { tab,position -> tab.text=getString(Ui.dayNames[position]) }.attach()
        if(savedInstanceState==null) pager.setCurrentItem((LocalDate.now().dayOfWeek.value-1).coerceAtMost(adapter.itemCount-1),false)
        findViewById<ExtendedFloatingActionButton>(R.id.add_course_button).setOnClickListener { startActivity(Intent(this,CourseActivity::class.java)) }
        NotificationUtils.createNotificationChannel(this)
    }
    override fun onResume() {
        super.onResume()
        NotificationUtils.enableCourseReminders(this,lifecycleScope)
    }
}
