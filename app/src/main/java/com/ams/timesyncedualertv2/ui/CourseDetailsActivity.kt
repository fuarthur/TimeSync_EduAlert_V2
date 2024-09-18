package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.util.NotificationUtils
import kotlinx.coroutines.launch

class CourseDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_details)

        val buttonBack = findViewById<Button>(R.id.button_back)
        val buttonEditCourse = findViewById<Button>(R.id.button_edit_course)
        val buttonDeleteCourse = findViewById<Button>(R.id.button_delete_course)

        val courseName = intent.getStringExtra("course_name")
        val courseLocation = intent.getStringExtra("course_location")
        val courseStartTime = intent.getStringExtra("course_start_time")
        val courseEndTime = intent.getStringExtra("course_end_time")
        val courseDescription = intent.getStringExtra("course_description")
        val courseId = intent.getIntExtra("course_id", -1)

        val courseNameTextView = findViewById<TextView>(R.id.textView_course_name)
        val courseLocationTextView = findViewById<TextView>(R.id.textView_course_location)
        val courseTimeTextView = findViewById<TextView>(R.id.textView_course_time)
        val courseDescriptionTextView = findViewById<TextView>(R.id.textView_course_description)

        courseNameTextView.text = courseName
        courseLocationTextView.text = courseLocation
        courseTimeTextView.text = getString(R.string.course_time_format, courseStartTime, courseEndTime)
        courseDescriptionTextView.text = courseDescription

        // 获取数据库实例
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "courses.db"
        ).build()
        val courseDao = db.courseDao()

        buttonBack.setOnClickListener {
            finish()
        }

        // 编辑课程点击事件
        buttonEditCourse.setOnClickListener {
            val intent = Intent(this, CourseActivity::class.java).apply {
                putExtra("course_id", courseId)
                putExtra("course_name", courseName)
                putExtra("course_location", courseLocation)
                putExtra("course_start_time", courseStartTime)
                putExtra("course_end_time", courseEndTime)
                putExtra("course_description", courseDescription)
            }
            startActivity(intent)
        }

        // 删除课程点击事件
        buttonDeleteCourse.setOnClickListener {
            lifecycleScope.launch {
                val course = courseDao.getCourseById(courseId)
                if (course != null) {
                    NotificationUtils.cancelNotificationForCourse(this@CourseDetailsActivity, course)
                    courseDao.delete(course)
                    NotificationUtils.refreshReminders(this@CourseDetailsActivity, lifecycleScope)
                    runOnUiThread {
                        Toast.makeText(this@CourseDetailsActivity, "Course deleted successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@CourseDetailsActivity, HomepageActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@CourseDetailsActivity, "Course not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
