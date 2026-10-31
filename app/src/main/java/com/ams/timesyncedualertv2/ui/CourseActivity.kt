package com.ams.timesyncedualertv2.ui

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.domain.CourseRules
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.NotificationUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CourseActivity:AppCompatActivity() {
    private var startTime=""
    private var endTime=""
    private var selectedColor=0xFF2456A6.toInt()
    private var courseId=-1
    private var draftLoaded=false
    private var initial=""
    private var saving=false
    private val repository get()=(application as TimeSyncApplication).repository
    private val days get()=listOf(R.id.checkbox_monday,R.id.checkbox_tuesday,R.id.checkbox_wednesday,R.id.checkbox_thursday,R.id.checkbox_friday,R.id.checkbox_saturday,R.id.checkbox_sunday)
    private fun field(id:Int)=findViewById<EditText>(id)
    private fun button(id:Int)=findViewById<Button>(id)
    private fun signature()=listOf(startTime,endTime,selectedColor.toString(),field(R.id.edittext_course_name).text.toString(),field(R.id.edittext_course_location).text.toString(),field(R.id.edittext_course_description).text.toString(),selectedDays().joinToString()).joinToString("\u0001")

    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_add_course)
        Ui.setup(this) { onBackPressedDispatcher.onBackPressed() }
        courseId=intent.getIntExtra("course_id",-1)
        val duplicateId=intent.getIntExtra("duplicate_id",-1)
        findViewById<MaterialToolbar>(R.id.toolbar).setTitle(if(courseId==-1) R.string.add_course else R.string.edit_course)
        field(R.id.edittext_course_description).inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        field(R.id.edittext_course_description).minLines=3
        field(R.id.edittext_course_description).maxLines=6
        draftLoaded=state?.getBoolean("draft_loaded") ?: (courseId==-1 && duplicateId==-1)
        if(state!=null) {
            startTime=state.getString("draft_start",""); endTime=state.getString("draft_end","")
            selectedColor=state.getInt("draft_color",selectedColor)
            initial=state.getString("draft_initial","")
        }
        updateTimeButtons(); updateColor()
        button(R.id.button_start_time).setOnClickListener { pickTime(startTime) { value ->
            startTime=value
            if(endTime.isBlank() || runCatching { CourseRules.time(endTime)<=CourseRules.time(startTime) }.getOrDefault(true)) {
                val later=CourseRules.time(startTime).plusHours(1)
                endTime=if(later>CourseRules.time(startTime)) format(later) else "23:59"
            }
            updateTimeButtons()
        } }
        button(R.id.button_end_time).setOnClickListener { pickTime(endTime.ifBlank { startTime }) { endTime=it; updateTimeButtons() } }
        button(R.id.button_select_color).setOnClickListener { chooseColor() }
        button(R.id.button_submit).setText(if(courseId==-1) R.string.save_course else R.string.save_changes)
        button(R.id.button_submit).setOnClickListener { save() }
        if(initial.isEmpty() && state==null) initial=signature()
        if((courseId!=-1 || duplicateId!=-1) && !draftLoaded) {
            button(R.id.button_submit).isEnabled=false
            lifecycleScope.launch {
                try {
                    val course=repository.get(if(courseId!=-1) courseId else duplicateId)
                    if(course==null) { Ui.message(this@CourseActivity,R.string.missing_course); finish(); return@launch }
                    fill(course)
                    if(duplicateId!=-1) { field(R.id.edittext_course_name).setText(getString(R.string.copy_name,course.name).take(120)); Ui.message(this@CourseActivity,R.string.duplicate_hint) }
                    draftLoaded=true; initial=signature()
                } catch(e:Exception) { if(e is CancellationException) throw e; Ui.error(this@CourseActivity,e) }
                finally { button(R.id.button_submit).isEnabled=true }
            }
        }
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(saving) return
                if(signature()==initial) finish() else MaterialAlertDialogBuilder(this@CourseActivity)
                    .setTitle(R.string.unsaved_title).setMessage(R.string.unsaved_message)
                    .setNegativeButton(R.string.keep_editing,null).setPositiveButton(R.string.discard) { _,_->finish() }.show()
            }
        })
    }

    private fun selectedDays()=days.mapIndexedNotNull { index,id -> if(findViewById<CheckBox>(id).isChecked) index+1 else null }
    private fun fill(c:CourseEntity) {
        field(R.id.edittext_course_name).setText(c.name); field(R.id.edittext_course_location).setText(c.location)
        field(R.id.edittext_course_description).setText(c.description)
        startTime=c.startTime; endTime=c.endTime; selectedColor=c.color
        days.forEachIndexed { index,id -> findViewById<CheckBox>(id).isChecked=index+1 in c.weekday }
        updateTimeButtons(); updateColor()
    }
    private fun updateTimeButtons() {
        button(R.id.button_start_time).text=startTime.ifBlank { getString(R.string.hint_select) }
        button(R.id.button_end_time).text=endTime.ifBlank { getString(R.string.hint_select) }
    }
    private fun updateColor() { button(R.id.button_select_color).apply { backgroundTintList=ColorStateList.valueOf(selectedColor); setTextColor(Ui.onColor(selectedColor)) } }
    private fun chooseColor() {
        val colors=intArrayOf(0xFF2456A6.toInt(),0xFF28704F.toInt(),0xFF76529C.toInt(),0xFFA84F17.toInt(),0xFFAC4263.toInt(),0xFF147C82.toInt(),0xFF526176.toInt(),0xFF8A6500.toInt())
        val labels=intArrayOf(R.string.color_blue,R.string.color_green,R.string.color_purple,R.string.color_orange,R.string.color_rose,R.string.color_teal,R.string.color_slate,R.string.color_gold).map(::getString).toTypedArray()
        MaterialAlertDialogBuilder(this).setTitle(R.string.choose_color).setSingleChoiceItems(labels,colors.indexOf(selectedColor)) { dialog,index -> selectedColor=colors[index]; updateColor(); dialog.dismiss() }.setNegativeButton(R.string.cancel,null).show()
    }
    private fun format(value:LocalTime)=value.format(DateTimeFormatter.ofPattern("HH:mm",Locale.ROOT))
    private fun pickTime(value:String,onChosen:(String)->Unit) {
        val current=runCatching { CourseRules.time(value) }.getOrElse { LocalTime.now() }
        TimePickerDialog(this,{ _,hour,minute -> onChosen(format(LocalTime.of(hour,minute))) },current.hour,current.minute,true).show()
    }
    private fun fail(message:Int) { findViewById<TextView>(R.id.form_error).apply { setText(message); visibility=View.VISIBLE; announceForAccessibility(text) } }
    private fun save() {
        if(saving || !draftLoaded) return
        val name=field(R.id.edittext_course_name).text.toString().trim()
        if(name.isBlank()) { field(R.id.edittext_course_name).error=getString(R.string.name_required); return }
        if(selectedDays().isEmpty()) { fail(R.string.days_required); return }
        if(startTime.isBlank() || endTime.isBlank()) { fail(R.string.time_required); return }
        if(runCatching { CourseRules.time(endTime)<=CourseRules.time(startTime) }.getOrDefault(true)) { fail(R.string.time_invalid); return }
        findViewById<View>(R.id.form_error).visibility=View.GONE
        val course=CourseEntity(if(courseId==-1) 0 else courseId,selectedDays(),startTime,endTime,name,field(R.id.edittext_course_location).text.toString(),field(R.id.edittext_course_description).text.toString(),selectedColor)
        saving=true; button(R.id.button_submit).isEnabled=false
        lifecycleScope.launch {
            try { repository.save(course); NotificationUtils.refreshReminders(this@CourseActivity,lifecycleScope); finish() }
            catch(e:Exception) { if(e is CancellationException) throw e; Ui.error(this@CourseActivity,e) }
            finally { saving=false; button(R.id.button_submit).isEnabled=true }
        }
    }
    override fun onSaveInstanceState(out:Bundle) {
        out.putString("draft_start",startTime); out.putString("draft_end",endTime); out.putInt("draft_color",selectedColor)
        out.putBoolean("draft_loaded",draftLoaded); out.putString("draft_initial",initial)
        super.onSaveInstanceState(out)
    }
}
