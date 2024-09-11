package com.ams.timesyncedualertv2.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ams.timesyncedualertv2.R
import yuku.ambilwarna.AmbilWarnaDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.CheckBox
import android.widget.EditText
import java.util.Calendar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.model.CourseEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AddCourseActivity : AppCompatActivity() {
    private var startTime: String = ""
    private var endTime: String = ""
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val checkboxMonday by lazy { findViewById<CheckBox>(R.id.checkbox_monday) }
    private val checkboxTuesday by lazy { findViewById<CheckBox>(R.id.checkbox_tuesday) }
    private val checkboxWednesday by lazy { findViewById<CheckBox>(R.id.checkbox_wednesday) }
    private val checkboxThursday by lazy { findViewById<CheckBox>(R.id.checkbox_thursday) }
    private val checkboxFriday by lazy { findViewById<CheckBox>(R.id.checkbox_friday) }

    private val editTextCourseName by lazy { findViewById<EditText>(R.id.edittext_course_name) }
    private val editTextLocation by lazy { findViewById<EditText>(R.id.edittext_course_location) }
    private val editTextDescription by lazy { findViewById<EditText>(R.id.edittext_course_description) }

    private val buttonSelectStartTime by lazy { findViewById<Button>(R.id.button_start_time) }
    private val buttonSelectEndTime by lazy { findViewById<Button>(R.id.button_end_time) }
    private var selectedColor: Int = Color.RED // Default color
    private val buttonSelectColor: Button by lazy { findViewById(R.id.button_select_color) }
    private val buttonSubmit: Button by lazy { findViewById(R.id.button_submit) }
    private val buttonBack: Button by lazy { findViewById(R.id.button_back) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_course)

        buttonSelectStartTime.setOnClickListener {
            showTimePickerDialog { selectedTime ->
                if (endTime.isBlank() || isEndTimeValid(selectedTime, endTime)) {
                    startTime = selectedTime
                    buttonSelectStartTime.text = startTime
                } else {
                    Toast.makeText(
                        this,
                        "Start time must be earlier than end time",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        buttonSelectEndTime.setOnClickListener {
            showTimePickerDialog { selectedTime ->
                if (isEndTimeValid(startTime, selectedTime)) {
                    endTime = selectedTime
                    buttonSelectEndTime.text = endTime
                } else {
                    Toast.makeText(
                        this,
                        "End time must be later than start time",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        buttonSelectColor.setBackgroundColor(selectedColor)
        buttonSelectColor.setOnClickListener {
            openColorPickerDialog()
        }

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "courses.db"
        ).build()

        val courseDao = db.courseDao()

        buttonSubmit.setOnClickListener {
            val weekdays = getSelectedWeekdays()
            val courseName = editTextCourseName.text.toString()
            val location = editTextLocation.text.toString()
            val description = editTextDescription.text.toString()

            if (!checkElements(weekdays, startTime, endTime, courseName, location, description)) {
                return@setOnClickListener
            }

            val courseEntity = CourseEntity(
                weekday = getSelectedWeekdays(),
                startTime = startTime,
                endTime = endTime,
                name = courseName,
                location = location,
                description = description,
                color = selectedColor
            )

            // 使用 lifecycleScope 代替 GlobalScope 启动协程
            lifecycleScope.launch {
                courseDao.insert(courseEntity)
                runOnUiThread {
                    Toast.makeText(
                        this@AddCourseActivity,
                        "Course added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@AddCourseActivity, HomepageActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        buttonBack.setOnClickListener {
            val intent = Intent(this, HomepageActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showTimePickerDialog(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime =
                String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(formattedTime)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun isEndTimeValid(startTime: String, endTime: String): Boolean {
        return try {
            val startDate = timeFormat.parse(startTime)
            val endDate = timeFormat.parse(endTime)
            startDate != null && endDate != null && endDate.after(startDate)
        } catch (e: Exception) {
            false
        }
    }

    private fun openColorPickerDialog() {
        val colorPicker =
            AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                    selectedColor = color
                    buttonSelectColor.setBackgroundColor(selectedColor)
                }

                override fun onCancel(dialog: AmbilWarnaDialog) {
                    // do nothing
                }
            })
        colorPicker.show()
    }

    private fun getSelectedWeekdays(): List<Int> {
        val weekdays = mutableListOf<Int>()
        if (checkboxMonday.isChecked) weekdays.add(1)   // Monday
        if (checkboxTuesday.isChecked) weekdays.add(2)  // Tuesday
        if (checkboxWednesday.isChecked) weekdays.add(3) // Wednesday
        if (checkboxThursday.isChecked) weekdays.add(4) // Thursday
        if (checkboxFriday.isChecked) weekdays.add(5)   // Friday
        return weekdays
    }

    private fun checkElements(
        weekdays: List<Int>,
        startTime: String,
        endTime: String,
        courseName: String,
        location: String,
        description: String
    ): Boolean {
        if (weekdays.isEmpty()) {
            Toast.makeText(this, "Please select at least one weekday", Toast.LENGTH_SHORT).show()
            return false
        }

        if (startTime.isBlank() || endTime.isBlank()) {
            Toast.makeText(this, "Please select start and end time", Toast.LENGTH_SHORT).show()
            return false
        }

        if (courseName.isBlank()) {
            Toast.makeText(this, "Please enter the course name", Toast.LENGTH_SHORT).show()
            return false
        }

        if (location.isBlank()) {
            Toast.makeText(this, "Please enter the location", Toast.LENGTH_SHORT).show()
            return false
        }

        if (description.isBlank()) {
            Toast.makeText(this, "Please enter the description", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}