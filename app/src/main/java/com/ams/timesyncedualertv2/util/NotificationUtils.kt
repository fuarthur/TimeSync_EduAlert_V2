package com.ams.timesyncedualertv2.util

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationUtils {

    // Function to enable course reminders based on reminder_time in preferences
    fun enableCourseReminders(context: Context, lifecycleScope: CoroutineScope) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "courses.db"
        ).build()
        val courseDao = db.courseDao()

        val sharedPreferences = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val reminderTime = sharedPreferences.getInt("reminder_time", 10)

        // Use lifecycleScope to handle this in the proper context
        lifecycleScope.launch(Dispatchers.IO) {
            val courses = courseDao.getAllCourses() // Fetch all courses

            for (course in courses) {
                createWeeklyNotification(context, course, reminderTime)
            }
        }
    }

    fun cancelNotificationForCourse(context: Context, course: CourseEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(course.id) // Cancel notification by course ID
    }

    // Function to cancel course reminders
    fun cancelCourseReminders(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "courses.db"
        ).build()

        val courseDao = db.courseDao()

        CoroutineScope(Dispatchers.IO).launch {
            val courses = courseDao.getAllCourses() // Fetch all courses
            for (course in courses) {
                // Cancel the notification using the course id as the notification id
                notificationManager.cancel(course.id)
            }
        }
    }

    // Private function to create weekly notifications for each course
    private fun createWeeklyNotification(context: Context, course: CourseEntity, reminderTime: Int) {
        val calendar = Calendar.getInstance()

        // Set the course weekday in the calendar (1 = Sunday, 7 = Saturday in Calendar API)
        calendar.set(Calendar.DAY_OF_WEEK, mapToCalendarDayOfWeek(course.weekday[0]))

        // Set the course start time
        val courseStartTime = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(course.startTime)
        courseStartTime?.let {
            val cal = Calendar.getInstance()
            cal.time = it
            calendar.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))

            // Subtract reminder time (in minutes)
            calendar.add(Calendar.MINUTE, -reminderTime)

            // Check if the current time is after the reminder time; if so, move the notification to next week
            if (Calendar.getInstance().after(calendar)) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            // Trigger the notification
            showCourseReminder(context, course)

            // Schedule a notification for next week
            scheduleNextWeekReminder(context, course)
        }
    }

    // Show the notification for the course
    private fun showCourseReminder(context: Context, course: CourseEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "course_reminder_channel")
            .setSmallIcon(R.drawable.icon) // Replace with actual notification icon
            .setContentTitle("Upcoming Course: ${course.name}")
            .setContentText("Starts at ${course.startTime}. Location: ${course.location}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Use course.id to uniquely identify the notification
        notificationManager.notify(course.id, notification)
    }

    // Schedule the notification for next week
    private fun scheduleNextWeekReminder(context: Context, course: CourseEntity) {
        // Set the next reminder for the same course on the same weekday and time next week
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Use NotificationCompat.Builder to build and trigger the notification at the calculated time
        val notification = NotificationCompat.Builder(context, "course_reminder_channel")
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Next Week Course: ${course.name}")
            .setContentText("Starts at ${course.startTime}. Location: ${course.location}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Schedule it for next week by setting the next course notification
        notificationManager.notify(course.id, notification)
    }

    // Utility function to refresh reminders after a course is added or modified
    fun refreshReminders(context: Context, lifecycleScope: CoroutineScope) {
        cancelCourseReminders(context) // Cancel existing reminders
        enableCourseReminders(context, lifecycleScope) // Re-enable new reminders
    }

    // Helper function to map custom weekday (1 for Monday, etc.) to Calendar day constants
    private fun mapToCalendarDayOfWeek(customWeekday: Int): Int {
        return when (customWeekday) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            else -> Calendar.SUNDAY // Default to Sunday if invalid
        }
    }
}
