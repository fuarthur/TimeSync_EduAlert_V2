package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ams.timesyncedualertv2.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar

class HomepageActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private val settingButton: Button by lazy { findViewById(R.id.settings_button) }
    private val addCourseButton: Button by lazy { findViewById(R.id.add_course_button) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        viewPager = findViewById(R.id.pager)
        tabLayout = findViewById(R.id.tab_layout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.weekdays[position]
        }.attach()

        switchToCurrentDayTab()


        settingButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        addCourseButton.setOnClickListener {
            val intent = Intent(this, CourseActivity::class.java)
            startActivity(intent)
        }
    }
    private fun switchToCurrentDayTab() {
        // 获取当前星期几，注意，Calendar 的 DAY_OF_WEEK 返回值为 1~7，对应周日到周六
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 将 DAY_OF_WEEK 映射到 adapter.weekdays 的索引 (Monday 为 0)
        val tabIndex = when (currentDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            else -> 0 // 如果今天是周六或周日，默认显示周一
        }

        // 切换到对应的 tab
        viewPager.currentItem = tabIndex
    }

}
