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
import com.ams.timesyncedualertv2.ui.HomepageActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime
import java.time.Instant
import android.util.Log

object NotificationUtils {
    private const val CHANNEL="timesync_edualert"
    private const val KEYS="scheduled_reminders"
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val mutex=Mutex()
    private fun settings(context:Context)=context.getSharedPreferences("user_settings",Context.MODE_PRIVATE)

    fun createNotificationChannel(context:Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,context.getString(R.string.notifications),NotificationManager.IMPORTANCE_HIGH)
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun enableCourseReminders(context:Context,lifecycleScope:CoroutineScope) {
        val app=context.applicationContext
        scope.launch {
            try { reschedule(app) }
            catch(e:Exception) { if(e is CancellationException) throw e; Log.e("TimeSync","Could not refresh reminders",e) }
        }
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
        val pending=pending(context,course.id,day,trigger)
        try {
            if (alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pending)
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pending)
        } catch (_:SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pending) }
        val keys=settings(context).getStringSet(KEYS,emptySet()).orEmpty().toMutableSet()
        keys.add("${course.id}/$day")
        settings(context).edit().putStringSet(KEYS,keys).putLong("trigger_${course.id}/$day",trigger).apply()
    }

    private fun pending(context:Context,id:Int,day:Int,trigger:Long=-1L)=PendingIntent.getBroadcast(context,0,
        Intent(context,CourseReminderReceiver::class.java).setData(Uri.parse("timesync://course/$id/$day"))
            .putExtra("course_id",id).putExtra("weekday",day).putExtra("trigger_at",trigger),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun cancelNotificationForCourse(context:Context,course:CourseEntity) {
        val manager=context.getSystemService(AlarmManager::class.java)
        (1..7).forEach { manager.cancel(pending(context,course.id,it)) }
        context.getSystemService(NotificationManager::class.java).cancel(course.id)
    }

    @Synchronized
    fun cancelCourseReminders(context:Context) {
        val alarm=context.getSystemService(AlarmManager::class.java)
        val editor=settings(context).edit().remove(KEYS)
        settings(context).getStringSet(KEYS,emptySet()).orEmpty().forEach { key ->
            val parts=key.split("/")
            val id=parts.getOrNull(0)?.toIntOrNull()
            val day=parts.getOrNull(1)?.toIntOrNull()
            if (id!=null && day!=null) { val token=pending(context,id,day); alarm.cancel(token); token.cancel() }
            editor.remove("trigger_$key")
        }
        editor.apply()
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }

    suspend fun handleAlarm(context:Context,id:Int,day:Int,trigger:Long,now:ZonedDateTime=ZonedDateTime.now()) = mutex.withLock {
        val preferences=settings(context)
        if(!preferences.getBoolean("enable_notifications",true) || trigger<=0 || preferences.getLong("trigger_$id/$day",-1L)!=trigger) return@withLock
        val course=AppDatabase.getInstance(context).courseDao().getCourseById(id) ?: return@withLock
        if(day !in course.weekday || runCatching { CourseRules.validate(course) }.isFailure) return@withLock
        val minutes=preferences.getInt("reminder_time",10).coerceIn(1,1440)
        val before=Instant.ofEpochMilli(trigger-1).atZone(now.zone)
        val currentTrigger=ReminderTime.next(course,day,minutes,before).toInstant().toEpochMilli()
        if(currentTrigger==trigger && now.toInstant().toEpochMilli()>=trigger) {
            val classDate=Instant.ofEpochMilli(trigger).atZone(now.zone).plusMinutes(minutes.toLong()).toLocalDate()
            val end=classDate.atTime(CourseRules.time(course.endTime)).atZone(now.zone)
            if(now.isBefore(end)) postReminder(context,course)
        }
        scheduleCourse(context,course,day,minutes,now)
    }

    fun refreshReminders(context:Context,lifecycleScope:CoroutineScope)=enableCourseReminders(context,lifecycleScope)

    fun postReminder(context:Context,course:CourseEntity) {
        if (!settings(context).getBoolean("enable_notifications",true)) return
        if (ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        createNotificationChannel(context)
        val intent=Intent(context,HomepageActivity::class.java).apply {
            flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_course_id",course.id)
        }
        val content=PendingIntent.getActivity(context,course.id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(course.name)
            .setContentText("${course.startTime} · ${course.location}")
            .setContentIntent(content).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(course.id,notification)
    }
}
