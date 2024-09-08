package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ams.timesyncedualertv2.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomepageActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var settingsButton: Button
    private lateinit var addCourseButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // 初始化 View
        viewPager = findViewById(R.id.pager)
        tabLayout = findViewById(R.id.tab_layout)

        // 设置 ViewPager 适配器
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // 将 TabLayout 和 ViewPager2 绑定
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // 设置 Tab 的文本为工作日
            tab.text = adapter.weekdays[position]
        }.attach()

//        // 设置按钮点击事件
//        settingsButton.setOnClickListener {
//            // 跳转到设置页面的逻辑
//            val intent = Intent(this, SettingsActivity::class.java)
//            startActivity(intent)
//        }
//
//        addCourseButton.setOnClickListener {
//            // 跳转到添加课程的页面
//            val intent = Intent(this, AddCourseActivity::class.java)
//            startActivity(intent)
//        }
    }
}
