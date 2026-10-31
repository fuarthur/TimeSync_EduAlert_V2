package com.ams.timesyncedualertv2.regression

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.ams.timesyncedualertv2.util.ReminderRescheduleReceiver
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RescheduleReceiverTest {
    @Test
    fun missingOrUnrelatedActionsDoNotStartRescheduling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(null, "com.ams.timesyncedualertv2.UNRELATED", Intent.ACTION_PACKAGE_ADDED)
            .forEach { action ->
                val receiver = ReminderRescheduleReceiver()
                receiver.onReceive(context, Intent().setAction(action))
                assertFalse("Unexpected background work for $action", shadowOf(receiver).wentAsync())
            }
    }
}
