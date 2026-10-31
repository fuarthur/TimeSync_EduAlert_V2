package com.ams.timesyncedualertv2.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.domain.CourseRules
import com.ams.timesyncedualertv2.domain.ReminderTime
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.ui.CourseDetailsActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime

object NotificationUtils {
    private const val CHANNEL="timesync_edualert"
    private const val KEYS="scheduled_reminders"
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val mutex=Mutex()
    private fun settings(context:Context)=context.getSharedPreferences("user_settings",Context.MODE_PRIVATE)

    fun createNotificationChannel(context:Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"Course reminders",NotificationManager.IMPORTANCE_HIGH)
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun enableCourseReminders(context:Context,lifecycleScope:CoroutineScope) {
        val app=context.applicationContext
        scope.launch { reschedule(app) }
    }

    suspend fun reschedule(context:Context) = mutex.withLock {
        cancelCourseReminders(context)
        if (!settings(context).getBoolean("enable_notifications",true)) return@withLock
        if (ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return@withLock
        val db=AppDatabase.getInstance(context.applicationContext)
        run {
            val minutes=settings(context).getInt("reminder_time",10).coerceIn(1,1440)
            db.courseDao().getAllCourses().forEach { course ->
                if (runCatching { CourseRules.validate(course) }.isSuccess) {
                    course.weekday.forEach { day -> scheduleCourse(context,course,day,minutes) }
                }
            }
        }
    }

    @Synchronized
    fun scheduleCourse(context:Context,course:CourseEntity,day:Int,minutes:Int,now:ZonedDateTime=ZonedDateTime.now()) {
        if (!settings(context).getBoolean("enable_notifications",true)) return
        val alarm=context.getSystemService(AlarmManager::class.java)
        val trigger=ReminderTime.next(course,day,minutes,now).toInstant().toEpochMilli()
        val pending=pending(context,course.id,day)
        try {
            if (alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pending)
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pending)
        } catch (_:SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pending) }
        val keys=settings(context).getStringSet(KEYS,emptySet()).orEmpty().toMutableSet()
        keys.add("${course.id}/$day")
        settings(context).edit().putStringSet(KEYS,keys).apply()
    }

    private fun pending(context:Context,id:Int,day:Int)=PendingIntent.getBroadcast(context,0,
        Intent(context,CourseReminderReceiver::class.java).setData(Uri.parse("timesync://course/$id/$day"))
            .putExtra("course_id",id).putExtra("weekday",day),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun cancelNotificationForCourse(context:Context,course:CourseEntity) {
        val manager=context.getSystemService(AlarmManager::class.java)
        (1..7).forEach { manager.cancel(pending(context,course.id,it)) }
        context.getSystemService(NotificationManager::class.java).cancel(course.id)
    }

    @Synchronized
    fun cancelCourseReminders(context:Context) {
        val alarm=context.getSystemService(AlarmManager::class.java)
        settings(context).getStringSet(KEYS,emptySet()).orEmpty().forEach { key ->
            val parts=key.split("/")
            val id=parts.getOrNull(0)?.toIntOrNull()
            val day=parts.getOrNull(1)?.toIntOrNull()
            if (id!=null && day!=null) alarm.cancel(pending(context,id,day))
        }
        settings(context).edit().remove(KEYS).apply()
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }

    fun refreshReminders(context:Context,lifecycleScope:CoroutineScope)=enableCourseReminders(context,lifecycleScope)

    fun postReminder(context:Context,course:CourseEntity) {
        if (!settings(context).getBoolean("enable_notifications",true)) return
        if (ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        createNotificationChannel(context)
        val intent=Intent(context,CourseDetailsActivity::class.java).apply {
            putExtra("course_id",course.id); putExtra("course_name",course.name)
            putExtra("course_location",course.location); putExtra("course_start_time",course.startTime)
            putExtra("course_end_time",course.endTime); putExtra("course_description",course.description)
        }
        val content=PendingIntent.getActivity(context,course.id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(course.name)
            .setContentText("${course.startTime} · ${course.location}")
            .setContentIntent(content).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(course.id,notification)
    }
}
