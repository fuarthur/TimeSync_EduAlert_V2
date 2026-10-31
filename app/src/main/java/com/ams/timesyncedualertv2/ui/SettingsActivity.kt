package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.db.CourseDao
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.NotificationUtils
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private val switchEnableNotifications: SwitchCompat by lazy { findViewById(R.id.switch_enable_notifications) }
    private val buttonBack: Button by lazy { findViewById(R.id.button_back) }
    private val buttonImportSchedule: Button by lazy { findViewById(R.id.button_import_schedule) }
    private val buttonExportSchedule: Button by lazy { findViewById(R.id.button_export_schedule) }
    private val buttonDeleteAllCourses: Button by lazy { findViewById(R.id.button_clear_all_schedules) }
    private val edittextTimeBefore: EditText by lazy { findViewById(R.id.edittext_time_before) }
    private val buttonSubmit: Button by lazy { findViewById(R.id.button_submit) }
    private lateinit var courseDao: CourseDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 获取数据库实例
        val db = AppDatabase.getInstance(applicationContext)
        courseDao = db.courseDao()

        val sharedPreferences = getSharedPreferences("user_settings", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val notificationsEnabled = sharedPreferences.getBoolean("enable_notifications", true)

        switchEnableNotifications.isChecked = notificationsEnabled

        switchEnableNotifications.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("enable_notifications", isChecked)
            editor.apply()
            if (isChecked) {
                editor.putBoolean("enable_notifications", true)
                NotificationUtils.enableCourseReminders(this, lifecycleScope)
            } else {
                editor.putBoolean("enable_notifications", false)
                NotificationUtils.cancelCourseReminders(this)

            }
            editor.apply()
        }

        edittextTimeBefore.hint =
            getString(R.string.reminder_time_text, sharedPreferences.getInt("reminder_time", 10))
        buttonSubmit.setOnClickListener {
            val reminderTime = edittextTimeBefore.text.toString().trim().toIntOrNull()
            // Check if the input is a valid number between 1 and 1440 (minutes)
            if (reminderTime != null && reminderTime in 1..1440) {

                // Save the new reminder time in SharedPreferences
                editor.putInt("reminder_time", reminderTime)
                editor.apply()

                Toast.makeText(
                    this,
                    "Reminder time updated to $reminderTime minutes before",
                    Toast.LENGTH_SHORT
                ).show()

                // Cancel all existing course reminders
                NotificationUtils.cancelCourseReminders(this)

                // Re-enable course reminders with the updated time
                NotificationUtils.enableCourseReminders(this, lifecycleScope)

            } else {
                // Show a message if the input is invalid
                Toast.makeText(
                    this,
                    "Invalid time. Please enter a number between 1 and 1440 minutes.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }



        buttonBack.setOnClickListener {
            finish()
        }

        // 设置导入按钮点击事件
        buttonImportSchedule.setOnClickListener {
            // 创建一个 AlertDialog 以进行二次确认
            AlertDialog.Builder(this)
                .setTitle("Confirm Import")
                .setMessage("Import schedule will overwrite all existing schedules. Are you sure you want to continue?")
                .setPositiveButton("Yes") { dialog, _ ->
                    openFilePickerForImport()
                    dialog.dismiss() // 关闭对话框
                }
                .setNegativeButton("No") { dialog, _ ->
                    // 用户取消操作，关闭对话框
                    dialog.dismiss()
                }
                .create()
                .show() // 显示对话框
        }

        // 设置导出按钮点击事件
        buttonExportSchedule.setOnClickListener {
            openFilePickerForExport()
        }

        buttonDeleteAllCourses.setOnClickListener {
            // 创建一个 AlertDialog 以进行二次确认
            AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete all schedules?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // 用户确认，执行删除操作
                    lifecycleScope.launch {
                        courseDao.deleteAllCourses()
                        runOnUiThread {
                            Toast.makeText(
                                this@SettingsActivity,
                                "All schedules have been deleted.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    dialog.dismiss() // 关闭对话框
                }
                .setNegativeButton("No") { dialog, _ ->
                    // 用户取消操作，关闭对话框
                    dialog.dismiss()
                }
                .create()
                .show() // 显示对话框
        }
    }

    // 使用 SAF 打开文件选择器进行导入
    private fun openFilePickerForImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"  // 只允许选择 JSON 文件
        }
        importFilePickerLauncher.launch(intent)
    }

    // 使用 SAF 打开文件选择器进行导出
    private fun openFilePickerForExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "courses.json")  // 默认文件名
        }
        exportFilePickerLauncher.launch(intent)
    }

    // 文件选择器的回调（导入）
    private val importFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.also { uri ->
                // 从选中的文件导入课程数据
                importCoursesFromUri(uri)
            }
        }
    }

    // 文件选择器的回调（导出）
    private val exportFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.also { uri ->
                // 将课程数据导出到选定的文件
                exportCoursesToUri(uri)
            }
        }
    }

    private val repository get()=(application as com.ams.timesyncedualertv2.TimeSyncApplication).repository
    private val backup get()=com.ams.timesyncedualertv2.data.ScheduleBackupService(this,repository)

    private fun importCoursesFromUri(uri:Uri) {
        lifecycleScope.launch {
            try {
                val courses=backup.read(uri)
                repository.replace(courses)
                NotificationUtils.refreshReminders(this@SettingsActivity,lifecycleScope)
                Toast.makeText(this@SettingsActivity,"Courses imported successfully",Toast.LENGTH_SHORT).show()
            } catch(e:Exception) {
                if(e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(this@SettingsActivity,e.message ?: "Could not import courses",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportCoursesToUri(uri:Uri) {
        lifecycleScope.launch {
            try {
                backup.write(uri)
                Toast.makeText(this@SettingsActivity,"Courses exported successfully",Toast.LENGTH_SHORT).show()
            } catch(e:Exception) {
                if(e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(this@SettingsActivity,"Could not export courses",Toast.LENGTH_LONG).show()
            }
        }
    }
}
