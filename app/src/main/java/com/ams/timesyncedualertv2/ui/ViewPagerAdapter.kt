package com.ams.timesyncedualertv2.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    val weekdays = listOf("Mon.", "Tue.", "Wed.", "Thu.", "Fri.")

    override fun getItemCount(): Int {
        return weekdays.size
    }

    override fun createFragment(position: Int): Fragment {
        return WeekdayFragment.newInstance(weekdays[position])
    }
}

