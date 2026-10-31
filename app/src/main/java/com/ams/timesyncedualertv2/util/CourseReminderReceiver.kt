package com.ams.timesyncedualertv2.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.domain.CourseRules
import kotlinx.coroutines.*

class CourseReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val result=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            val db=AppDatabase.getInstance(context.applicationContext)
            try {
                val preferences=context.getSharedPreferences("user_settings",Context.MODE_PRIVATE)
                if (!preferences.getBoolean("enable_notifications",true)) return@launch
                val day=intent.getIntExtra("weekday",-1)
                val course=db.courseDao().getCourseById(intent.getIntExtra("course_id",-1)) ?: return@launch
                if (day !in course.weekday || runCatching { CourseRules.validate(course) }.isFailure) return@launch
                NotificationUtils.postReminder(context,course)
                NotificationUtils.scheduleCourse(context,course,day,preferences.getInt("reminder_time",10).coerceIn(1,1440))
            } finally { result.finish() }
        }
    }
}

class ReminderRescheduleReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val result=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try { NotificationUtils.reschedule(context.applicationContext) } finally { result.finish() }
        }
    }
}
