package com.ams.timesyncedualertv2.ui

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 7

    override fun createFragment(position: Int) = WeekdayFragment.newInstance(position + 1)
}
