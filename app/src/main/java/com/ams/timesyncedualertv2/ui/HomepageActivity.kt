package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.viewpager2.widget.ViewPager2
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

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

        // TODO: Delete this after testing
        settingButton.setOnClickListener {
            debugDeleteAll()
        }

        addCourseButton.setOnClickListener {
            val intent = Intent(this, AddCourseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun debugDeleteAll() {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "courses.db"
        ).build()

        val courseDao = db.courseDao()

        lifecycleScope.launch {
            courseDao.deleteAllCourses()
            Toast.makeText(this@HomepageActivity, "All courses deleted", Toast.LENGTH_SHORT).show()
            recreate()
        }
    }
}
