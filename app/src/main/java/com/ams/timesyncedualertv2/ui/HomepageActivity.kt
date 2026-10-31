package com.ams.timesyncedualertv2.ui

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.ams.timesyncedualertv2.domain.BackupCodec
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.domain.Timetable
import com.ams.timesyncedualertv2.model.CourseEntity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.util.NotificationUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

class HomepageActivity:AppCompatActivity() {
    val searchQuery=MutableStateFlow("")
    private var allCourses=emptyList<CourseEntity>()
    private val details=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val json=result.data?.getStringExtra("deleted_course")
        if(result.resultCode==RESULT_OK && json!=null) {
            val deleted=runCatching { BackupCodec.decode("[$json]").single() }.getOrNull()
            if(deleted!=null) Snackbar.make(findViewById(R.id.root),R.string.course_deleted,Snackbar.LENGTH_LONG)
                .setAnchorView(findViewById(R.id.add_course_button)).setAction(R.string.undo) {
                    lifecycleScope.launch {
                        try { (application as TimeSyncApplication).repository.restore(deleted); NotificationUtils.refreshReminders(this@HomepageActivity,lifecycleScope); Ui.message(this@HomepageActivity,R.string.course_restored) }
                        catch(e:Exception) { if(e is CancellationException) throw e; Ui.message(this@HomepageActivity,R.string.deleted_restore_failed) }
                    }
                }.show()
        }
    }
    fun openCourse(course:CourseEntity) { details.launch(Intent(this,CourseDetailsActivity::class.java).putExtra("course_id",course.id)) }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        Ui.setup(this)
        val toolbar=findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.subtitle=DateFormat.getDateInstance(DateFormat.FULL).format(Date())
        toolbar.menu.add(0,1,0,R.string.settings).apply { setIcon(R.drawable.ic_settings); icon?.setTint(com.google.android.material.color.MaterialColors.getColor(toolbar,com.google.android.material.R.attr.colorOnSurface)); setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS) }
        toolbar.setOnMenuItemClickListener { startActivity(Intent(this,SettingsActivity::class.java)); true }
        val pager=findViewById<ViewPager2>(R.id.pager)
        val adapter=ViewPagerAdapter(this)
        pager.adapter=adapter
        TabLayoutMediator(findViewById<TabLayout>(R.id.tab_layout),pager) { tab,position -> tab.text=getString(Ui.dayNames[position]) }.attach()
        if(savedInstanceState==null) pager.setCurrentItem((LocalDate.now().dayOfWeek.value-1).coerceAtMost(adapter.itemCount-1),false)
        findViewById<ExtendedFloatingActionButton>(R.id.add_course_button).setOnClickListener { startActivity(Intent(this,CourseActivity::class.java)) }
        findViewById<View>(R.id.search_container).visibility=View.VISIBLE
        val search=findViewById<TextInputEditText>(R.id.search_input)
        search.setText(savedInstanceState?.getString("search_query", "") ?: "")
        searchQuery.value=search.text.toString()
        search.doAfterTextChanged { searchQuery.value=it.toString() }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { while(isActive) { renderNext(); delay(30_000) } }
            (application as TimeSyncApplication).repository.courses.collect { allCourses=it; renderNext() }
        } }
        openRequestedCourse(intent)
        NotificationUtils.createNotificationChannel(this)
    }
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestedCourse(intent)
    }
    private fun openRequestedCourse(incoming:Intent) {
        val id=incoming.getIntExtra("open_course_id",-1)
        incoming.removeExtra("open_course_id")
        if(id<=0) return
        findViewById<View>(R.id.root).post {
            lifecycleScope.launch {
                try {
                    val course=(application as TimeSyncApplication).repository.get(id)
                    if(course!=null) openCourse(course) else Ui.message(this@HomepageActivity,R.string.missing_course)
                } catch(e:Exception) { if(e is CancellationException) throw e; Ui.error(this@HomepageActivity,e) }
            }
        }
    }
    private fun renderNext() {
        val card=findViewById<View>(R.id.next_course_card)
        val next=Timetable.next(allCourses,ZonedDateTime.now())
        card.visibility=if(next==null) View.GONE else View.VISIBLE
        if(next!=null) {
            findViewById<TextView>(R.id.next_course_title).apply { text=getString(if(next.ongoing) R.string.ongoing_course else R.string.next_course,next.course.name); maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END }
            val date=next.start.format(DateTimeFormatter.ofPattern("EEE, MMM d",Locale.getDefault()))
            findViewById<TextView>(R.id.next_course_info).text=date+"  "+getString(R.string.course_time_format,next.course.startTime,next.course.endTime)+next.course.location.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
            card.setOnClickListener { openCourse(next.course) }
        }
    }
    override fun onSaveInstanceState(out:Bundle) { out.putString("search_query",searchQuery.value); super.onSaveInstanceState(out) }
    override fun onResume() {
        super.onResume()
        NotificationUtils.enableCourseReminders(this,lifecycleScope)
    }
}
