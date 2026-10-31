package com.ams.timesyncedualertv2.regression

import android.widget.Button
import android.widget.EditText
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.ui.SettingsActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderInputTest {
    @Test
    fun overflowingMinutesAreRejectedWithoutCrashOrPreferenceChange() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        c.get().findViewById<EditText>(R.id.edittext_time_before).setText("9999999999")
        c.get().findViewById<Button>(R.id.button_submit).performClick()
        assertEquals(
            10,
            c.get().getSharedPreferences("user_settings", 0).getInt("reminder_time", 10),
        )
        c.pause().stop().destroy()
    }
}
