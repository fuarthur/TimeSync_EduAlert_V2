package com.ams.timesyncedualertv2.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.*

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationUtils.handleAlarm(
                    context.applicationContext,
                    intent.getIntExtra("course_id", -1),
                    intent.getIntExtra("weekday", -1),
                    intent.getLongExtra("trigger_at", -1L),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TimeSync", "Could not deliver course reminder", e)
            } finally {
                result.finish()
            }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationUtils.reschedule(context.applicationContext)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("TimeSync", "Could not reschedule reminders", e)
            } finally {
                result.finish()
            }
        }
    }
}
