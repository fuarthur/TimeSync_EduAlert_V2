package com.ams.timesyncedualertv2.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.data.ScheduleBackupService
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.NotificationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SettingsActivity:AppCompatActivity() {
    private val repository get()=(application as TimeSyncApplication).repository
    private val backup get()=ScheduleBackupService(this,repository)
    private val preferences get()=getSharedPreferences("user_settings",MODE_PRIVATE)
    private var pendingUri:String?=null
    private var busy=false
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        findViewById<MaterialSwitch>(R.id.switch_enable_notifications).isChecked=granted
        if(granted) NotificationUtils.enableCourseReminders(this,lifecycleScope)
        updateStatus()
    }
    private val importer=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) {
        try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch(_:SecurityException) { }
        readPreview(uri)
    } }
    private val exporter=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) launchBackup(R.string.export_failed) { backup.write(uri); Ui.message(this,R.string.exported) } }

    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_settings)
        Ui.setup(this) { finish() }
        val toggle=findViewById<MaterialSwitch>(R.id.switch_enable_notifications)
        toggle.isChecked=preferences.getBoolean("enable_notifications",true)
        toggle.setOnCheckedChangeListener { _,checked ->
            preferences.edit().putBoolean("enable_notifications",checked).apply()
            if(!checked) NotificationUtils.cancelCourseReminders(this)
            else if(ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) NotificationUtils.enableCourseReminders(this,lifecycleScope)
            else permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val minutes=findViewById<EditText>(R.id.edittext_time_before)
        minutes.inputType=InputType.TYPE_CLASS_NUMBER
        if(state==null) minutes.setText(preferences.getInt("reminder_time",10).toString())
        findViewById<Button>(R.id.button_submit).setOnClickListener {
            val value=minutes.text.toString().trim().toIntOrNull()
            if(value==null || value !in 1..1440) minutes.error=getString(R.string.invalid_minutes)
            else { minutes.error=null; preferences.edit().putInt("reminder_time",value).apply(); NotificationUtils.refreshReminders(this,lifecycleScope); Ui.message(this,R.string.reminder_saved) }
        }
        findViewById<Button>(R.id.notification_permission_button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))
        }
        findViewById<Button>(R.id.timing_permission_button).setOnClickListener {
            runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName"))) }.onFailure { Ui.message(this,R.string.operation_failed) }
        }
        findViewById<Button>(R.id.button_import_schedule).setOnClickListener { if(!busy) importer.launch(arrayOf("application/json","text/json","text/plain")) }
        findViewById<Button>(R.id.button_export_schedule).setOnClickListener { if(!busy) exporter.launch("timesync-timetable.json") }
        findViewById<Button>(R.id.button_clear_all_schedules).setOnClickListener {
            if(!busy) MaterialAlertDialogBuilder(this).setTitle(R.string.clear_title).setMessage(R.string.clear_message)
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.clear_schedule) { _,_-> launchBackup { repository.clear(); NotificationUtils.refreshReminders(this,lifecycleScope); Ui.message(this,R.string.cleared) } }.show()
        }
        state?.getString("pending_import_uri")?.let { findViewById<android.view.View>(R.id.root).post { readPreview(Uri.parse(it)) } }
    }
    override fun onResume() { super.onResume(); updateStatus(); NotificationUtils.enableCourseReminders(this,lifecycleScope) }
    private fun updateStatus() {
        findViewById<TextView>(R.id.notification_status).setText(if(NotificationManagerCompat.from(this).areNotificationsEnabled()) R.string.permission_on else R.string.permission_off)
        val exact=getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        findViewById<TextView>(R.id.timing_status).setText(if(exact) R.string.timing_exact else R.string.timing_approximate)
        findViewById<Button>(R.id.timing_permission_button).isEnabled=!exact
    }
    private fun launchBackup(errorMessage:Int=R.string.operation_failed,action:suspend ()->Unit) {
        if(busy) return
        busy=true; backupButtons(false)
        lifecycleScope.launch {
            try { action() }
            catch(e:Exception) { if(e is CancellationException) throw e; Ui.message(this@SettingsActivity,errorMessage) }
            finally { busy=false; backupButtons(true) }
        }
    }
    private fun backupButtons(enabled:Boolean) { listOf(R.id.button_import_schedule,R.id.button_export_schedule,R.id.button_clear_all_schedules).forEach { findViewById<Button>(it).isEnabled=enabled } }
    private fun readPreview(uri:Uri) {
        launchBackup {
            try {
                val courses=backup.read(uri)
                pendingUri=uri.toString()
                showPreview(courses)
            } catch(e:Exception) { if(e is CancellationException) throw e; pendingUri=null; Ui.message(this,R.string.import_failed) }
        }
    }
    private fun showPreview(courses:List<CourseEntity>) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.import_title).setMessage(getString(R.string.import_message,courses.size))
            .setNegativeButton(R.string.cancel) { _,_->pendingUri=null }.setOnCancelListener { pendingUri=null }
            .setPositiveButton(R.string.replace) { _,_->pendingUri=null; launchBackup(R.string.import_failed) { repository.replace(courses); NotificationUtils.refreshReminders(this,lifecycleScope); Ui.message(this,R.string.imported) } }.show()
    }
    override fun onSaveInstanceState(out:Bundle) { out.putString("pending_import_uri",pendingUri); super.onSaveInstanceState(out) }
}
