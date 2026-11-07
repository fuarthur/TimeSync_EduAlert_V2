package com.ams.timesyncedualertv2.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.domain.BackupCodec
import com.ams.timesyncedualertv2.domain.Timetable
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.util.NotificationUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class HomepageActivity : AppCompatActivity() {
    val searchQuery = MutableStateFlow("")
    private var allCourses = emptyList<CourseEntity>()
    private var keyboardVisible = false
    private val details =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val json = result.data?.getStringExtra("deleted_course")
            if (result.resultCode == RESULT_OK && json != null) {
                val deleted = runCatching { BackupCodec.decode("[$json]").single() }.getOrNull()
                if (deleted != null)
                    Snackbar.make(
                            findViewById(R.id.root),
                            R.string.course_deleted,
                            Snackbar.LENGTH_LONG,
                        )
                        .setAnchorView(findViewById(R.id.add_course_button))
                        .setAction(R.string.undo) {
                            lifecycleScope.launch {
                                try {
                                    (application as TimeSyncApplication).repository.restore(deleted)
                                    NotificationUtils.refreshReminders(
                                        this@HomepageActivity,
                                        lifecycleScope,
                                    )
                                    Ui.message(this@HomepageActivity, R.string.course_restored)
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    Ui.message(
                                        this@HomepageActivity,
                                        R.string.deleted_restore_failed,
                                    )
                                }
                            }
                        }
                        .show()
            }
        }

    fun openCourse(course: CourseEntity) {
        details.launch(
            Intent(this, CourseDetailsActivity::class.java).putExtra("course_id", course.id),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        Ui.setup(
            this,
            onInsets = { insets ->
                keyboardVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
                renderNext()
            },
        )
        findViewById<View>(R.id.root).addOnLayoutChangeListener {
            _,
            left,
            top,
            right,
            bottom,
            oldLeft,
            oldTop,
            oldRight,
            oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)
                renderNext()
            if (right - left != oldRight - oldLeft) {
                val tabs = findViewById<TabLayout>(R.id.tab_layout)
                tabs.post {
                    tabs.setScrollPosition(
                        findViewById<ViewPager2>(R.id.pager).currentItem,
                        0f,
                        true,
                    )
                }
            }
        }
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.subtitle = DateFormat.getDateInstance(DateFormat.FULL).format(Date())
        toolbar.setSubtitleTextAppearance(
            this,
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
        toolbar.setSubtitleTextColor(
            com.google.android.material.color.MaterialColors.getColor(
                toolbar,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
            ),
        )
        toolbar.menu.add(0, 1, 0, R.string.settings).apply {
            setIcon(R.drawable.ic_settings)
            icon
                ?.mutate()
                ?.setTint(
                    com.google.android.material.color.MaterialColors.getColor(
                        toolbar,
                        com.google.android.material.R.attr.colorOnSurface,
                    ),
                )
            setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        toolbar.setOnMenuItemClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        val pager = findViewById<ViewPager2>(R.id.pager)
        val adapter = ViewPagerAdapter(this)
        pager.adapter = adapter
        TabLayoutMediator(findViewById<TabLayout>(R.id.tab_layout), pager) { tab, position ->
                tab.text = getString(Ui.dayNames[position])
            }
            .attach()
        if (savedInstanceState == null)
            pager.setCurrentItem(
                (LocalDate.now().dayOfWeek.value - 1).coerceAtMost(adapter.itemCount - 1),
                false,
            )
        findViewById<ExtendedFloatingActionButton>(R.id.add_course_button).setOnClickListener {
            startActivity(Intent(this, CourseActivity::class.java))
        }
        findViewById<View>(R.id.search_container).visibility = View.VISIBLE
        val search = findViewById<TextInputEditText>(R.id.search_input)
        search.setText(savedInstanceState?.getString("search_query", "") ?: "")
        searchQuery.value = search.text.toString()
        search.doAfterTextChanged { searchQuery.value = it.toString() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    while (isActive) {
                        renderNext()
                        delay(30_000)
                    }
                }
                (application as TimeSyncApplication).repository.courses.collect {
                    allCourses = it
                    renderNext()
                }
            }
        }
        openRequestedCourse(intent)
        NotificationUtils.createNotificationChannel(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestedCourse(intent)
    }

    private fun openRequestedCourse(incoming: Intent) {
        val id = incoming.getIntExtra("open_course_id", -1)
        incoming.removeExtra("open_course_id")
        if (id <= 0) return
        findViewById<View>(R.id.root).post {
            lifecycleScope.launch {
                try {
                    val course = (application as TimeSyncApplication).repository.get(id)
                    if (course != null) openCourse(course)
                    else Ui.message(this@HomepageActivity, R.string.missing_course)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Ui.error(this@HomepageActivity, e)
                }
            }
        }
    }

    private fun renderNext() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(Date())
        if (toolbar.subtitle?.toString() != currentDate) toolbar.subtitle = currentDate
        val card = findViewById<View>(R.id.next_course_card)
        val next = Timetable.next(allCourses, ZonedDateTime.now())
        val root = findViewById<View>(R.id.root)
        val tabs = findViewById<TabLayout>(R.id.tab_layout)
        val width =
            (root.width - root.paddingLeft - root.paddingRight) / resources.displayMetrics.density
        if (width > 0) {
            val mode = if (width < 336) TabLayout.MODE_SCROLLABLE else TabLayout.MODE_FIXED
            if (tabs.tabMode != mode) tabs.tabMode = mode
            val gravity =
                if (mode == TabLayout.MODE_FIXED) TabLayout.GRAVITY_FILL
                else TabLayout.GRAVITY_CENTER
            if (tabs.tabGravity != gravity) tabs.tabGravity = gravity
        }
        val height =
            (root.height - root.paddingTop - root.paddingBottom) / resources.displayMetrics.density
        toolbar.visibility =
            if (keyboardVisible && height < 420) View.GONE else View.VISIBLE
        tabs.visibility =
            if (keyboardVisible && height < 300) View.GONE else View.VISIBLE
        findViewById<View>(R.id.add_course_button).visibility =
            if (keyboardVisible) View.GONE else View.VISIBLE
        card.visibility =
            if (next == null || keyboardVisible || height < 520) View.GONE else View.VISIBLE
        if (next != null) {
            val title = findViewById<TextView>(R.id.next_course_title)
            val titleText =
                getString(
                    if (next.ongoing) R.string.ongoing_course else R.string.next_course,
                    next.course.name,
                )
            if (title.text.toString() != titleText) title.text = titleText
            val date =
                next.start.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
            val info = findViewById<TextView>(R.id.next_course_info)
            val infoText =
                date +
                    "  " +
                    getString(
                        R.string.course_time_format,
                        next.course.startTime,
                        next.course.endTime,
                    ) +
                    next.course.location.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
            if (info.text.toString() != infoText) info.text = infoText
            card.setOnClickListener { openCourse(next.course) }
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        out.putString("search_query", searchQuery.value)
        super.onSaveInstanceState(out)
    }

    override fun onResume() {
        super.onResume()
        NotificationUtils.enableCourseReminders(this, lifecycleScope)
    }
}
