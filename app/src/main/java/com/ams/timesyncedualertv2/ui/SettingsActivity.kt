package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.db.CourseDao
import com.ams.timesyncedualertv2.model.CourseEntity
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SettingsActivity : AppCompatActivity() {
    private val switchEnableNotifications: Switch by lazy { findViewById(R.id.switch_enable_notifications) }
    private val buttonBack: Button by lazy { findViewById(R.id.button_back) }
    private val buttonImportSchedule: Button by lazy { findViewById(R.id.button_import_schedule) }
    private val buttonExportSchedule: Button by lazy { findViewById(R.id.button_export_schedule) }
    private val buttonDeleteAllCourses: Button by lazy { findViewById(R.id.button_clear_all_schedules) }
    private lateinit var courseDao: CourseDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 获取数据库实例
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "courses.db"
        ).build()
        courseDao = db.courseDao()

        // TODO: 设置开关状态，并监听状态变化（保存用户设置）

        buttonBack.setOnClickListener {
            intent = Intent(this, HomepageActivity::class.java)
            startActivity(intent)
        }

        // 设置导入按钮点击事件
        buttonImportSchedule.setOnClickListener {
            // 创建一个 AlertDialog 以进行二次确认
            AlertDialog.Builder(this)
                .setTitle("Confirm Import")
                .setMessage("Import schedule will overwrite all existing schedules. Are you sure you want to continue?")
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
                            openFilePickerForImport()
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

    // 从文件 URI 中读取数据并导入课程
    private fun importCoursesFromUri(uri: Uri) {
        val contentResolver = contentResolver
        lifecycleScope.launch {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val json = reader.readText()
                    reader.close()

                    // 将 JSON 数据转换为 CourseEntity 对象
                    val gson = Gson()
                    val importedCourses = gson.fromJson(json, Array<CourseEntity>::class.java).toList()

                    // 插入到数据库中
                    val db = Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java, "courses.db"
                    ).build()
                    val courseDao = db.courseDao()
                    courseDao.insertAll(importedCourses)

                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Courses imported successfully.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.d("User-debug", e.message.toString())
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Failed to import courses.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 将课程导出到用户选择的文件路径
    private fun exportCoursesToUri(uri: Uri) {
        val contentResolver = contentResolver
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "courses.db"
        ).build()
        val courseDao = db.courseDao()

        lifecycleScope.launch {
            val courses = courseDao.getAllCourses()  // 获取所有课程
            if (courses.isNotEmpty()) {
                val gson = Gson()
                val json = gson.toJson(courses)  // 将课程转换为 JSON

                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val writer = OutputStreamWriter(outputStream)
                        writer.write(json)
                        writer.flush()
                        writer.close()

                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Courses exported successfully.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Failed to export courses.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "No courses to export.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
