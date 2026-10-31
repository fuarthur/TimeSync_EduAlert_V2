package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.NotificationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CourseDetailsActivity:AppCompatActivity() {
    private var current:CourseEntity?=null
    private var deleting=false
    private val repository get()=(application as TimeSyncApplication).repository
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_course_details)
        Ui.setup(this) { finish() }
        val id=intent.getIntExtra("course_id",-1)
        findViewById<Button>(R.id.button_edit_course).setOnClickListener { startActivity(Intent(this,CourseActivity::class.java).putExtra("course_id",id)) }
        findViewById<Button>(R.id.button_duplicate_course).apply {
            visibility=android.view.View.VISIBLE
            setOnClickListener { startActivity(Intent(this@CourseDetailsActivity,CourseActivity::class.java).putExtra("duplicate_id",id)) }
        }
        findViewById<Button>(R.id.button_delete_course).setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle(R.string.delete_course).setMessage(R.string.delete_confirm)
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.delete) { _,_->deleteCourse(id) }.show()
        }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            repository.courses.collect { courses ->
                val c=courses.firstOrNull { it.id==id }
                if(c==null) { if(!deleting) finish(); return@collect }
                current=c
                findViewById<TextView>(R.id.textView_course_name).text=c.name
                findViewById<TextView>(R.id.textView_course_time).text=c.weekday.filter { it in 1..7 }.joinToString(", ") { getString(Ui.dayNames[it-1]) }+"\n"+getString(R.string.course_time_format,c.startTime,c.endTime)
                findViewById<TextView>(R.id.textView_course_location).text=c.location.ifBlank { getString(R.string.no_location) }
                findViewById<TextView>(R.id.textView_course_description).text=c.description.ifBlank { getString(R.string.no_notes) }
            }
        } }
    }
    private fun deleteCourse(id:Int) {
        if(deleting) return
        deleting=true
        lifecycleScope.launch {
            try {
                val deleted=repository.delete(id)
                if(deleted!=null) setResult(RESULT_OK,Intent().putExtra("deleted_course",com.google.gson.Gson().toJson(deleted)))
                NotificationUtils.refreshReminders(this@CourseDetailsActivity,lifecycleScope)
                finish()
            }
            catch(e:Exception) { if(e is CancellationException) throw e; deleting=false; Ui.error(this@CourseDetailsActivity,e) }
        }
    }
}
