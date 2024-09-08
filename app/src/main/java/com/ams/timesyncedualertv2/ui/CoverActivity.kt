package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.ams.timesyncedualertv2.R

class CoverActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover)

        Handler(Looper.myLooper()!!).postDelayed({
            startHomepageActivity()
        }, resources.getInteger(R.integer.cover_delay_milis).toLong())
    }

    private fun startHomepageActivity() {
        val intent = Intent(this, HomepageActivity::class.java)
        startActivity(intent)
    }
}