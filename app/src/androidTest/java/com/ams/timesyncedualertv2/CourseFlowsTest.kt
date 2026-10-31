package com.ams.timesyncedualertv2

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TimePicker
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ams.timesyncedualertv2.model.CourseEntity
import com.ams.timesyncedualertv2.ui.CourseActivity
import com.ams.timesyncedualertv2.ui.HomepageActivity
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseFlowsTest {
    private val app
        get() = ApplicationProvider.getApplicationContext<TimeSyncApplication>()

    @Before
    fun resetTestData() = runBlocking {
        app.getSharedPreferences("user_settings", 0)
            .edit()
            .putBoolean("enable_notifications", false)
            .commit()
        app.repository.clear()
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 8000
        while (!condition()) {
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for app state" }
            SystemClock.sleep(40)
        }
    }

    private fun count() = runBlocking { app.repository.all().size }

    private fun seed(
        day: Int = LocalDate.now().dayOfWeek.value,
        name: String = "Algorithms",
        room: String = "Siebel 1404",
    ) = runBlocking {
        app.repository.save(
            CourseEntity(
                weekday = listOf(day),
                startTime = "09:00",
                endTime = "10:00",
                name = name,
                location = room,
                description = "Bring a notebook",
                color = 0xFF2456A6.toInt(),
            ),
        )
    }

    private fun reveal() =
        object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)

            override fun getDescription() = "Reveal a control inside the scrolling form"

            override fun perform(ui: UiController, view: View) {
                view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
                ui.loopMainThreadUntilIdle()
            }
        }

    private fun setTime(hour: Int, minute: Int) =
        object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(TimePicker::class.java)

            override fun getDescription() = "Choose a course time"

            override fun perform(ui: UiController, view: View) {
                (view as TimePicker).apply {
                    this.hour = hour
                    this.minute = minute
                }
                ui.loopMainThreadUntilIdle()
            }
        }

    @Test
    fun weekendDraftSurvivesRotationAndSaves() {
        ActivityScenario.launch(CourseActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                a.findViewById<EditText>(R.id.edittext_course_name).setText("Weekend seminar")
                a.findViewById<CheckBox>(R.id.checkbox_saturday).isChecked = true
            }
            onView(withId(R.id.button_start_time)).perform(reveal(), click())
            onView(isAssignableFrom(TimePicker::class.java))
                .inRoot(isDialog())
                .perform(setTime(9, 0))
            onView(withText(android.R.string.ok)).perform(click())
            scenario.recreate()
            onView(withId(R.id.edittext_course_name)).check(matches(withText("Weekend seminar")))
            scenario.onActivity { it.findViewById<Button>(R.id.button_submit).performClick() }
            waitFor { count() == 1 }
            val c = runBlocking { app.repository.all().single() }
            assertEquals(listOf(6), c.weekday)
            assertEquals("09:00", c.startTime)
            assertEquals("10:00", c.endTime)
        }
    }

    @Test
    fun oneTapDeleteAndUndoRestoresCourse() {
        seed()
        ActivityScenario.launch(HomepageActivity::class.java).use {
            onView(allOf(withId(R.id.textView_course_name), isDisplayed())).perform(click())
            onView(withId(R.id.button_delete_course)).perform(reveal(), click())
            onView(withText(R.string.delete)).inRoot(isDialog()).perform(click())
            waitFor { count() == 0 }
            onView(withText(R.string.undo)).perform(click())
            waitFor { count() == 1 }
            assertEquals("Algorithms", runBlocking { app.repository.all().single().name })
        }
    }

    @Test
    fun systemBackAfterClearShowsCurrentData() {
        seed()
        ActivityScenario.launch(HomepageActivity::class.java).use {
            onView(withContentDescription(R.string.settings)).perform(click())
            onView(withId(R.id.button_clear_all_schedules)).perform(reveal(), click())
            onView(withText(R.string.clear_schedule)).inRoot(isDialog()).perform(click())
            waitFor { count() == 0 }
            pressBack()
            onView(allOf(withId(R.id.empty_title), isDisplayed()))
                .check(matches(withText(R.string.empty_day)))
        }
    }

    @Test
    fun weekendSearchFiltersCourseAndShowsEmptyResult() {
        seed(6, "Studio art", "Art studio")
        runBlocking {
            app.repository.save(
                CourseEntity(
                    weekday = listOf(6),
                    startTime = "11:00",
                    endTime = "12:00",
                    name = "Algorithms",
                    location = "Siebel",
                    description = "",
                    color = -65536,
                ),
            )
        }
        ActivityScenario.launch(HomepageActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.pager)
                    .setCurrentItem(5, false)
            }
            onView(withId(R.id.search_input)).perform(replaceText("studio"), closeSoftKeyboard())
            waitFor {
                var size = -1
                scenario.onActivity { a ->
                    size =
                        a.supportFragmentManager.fragments
                            .filterIsInstance<com.ams.timesyncedualertv2.ui.WeekdayFragment>()
                            .firstOrNull { it.arguments?.getInt("weekday") == 6 }
                            ?.view
                            ?.findViewById<androidx.recyclerview.widget.RecyclerView>(
                                R.id.courses_recycler_view,
                            )
                            ?.adapter
                            ?.itemCount ?: -1
                }
                size == 1
            }
            onView(allOf(withId(R.id.textView_course_name), withText("Studio art"), isDisplayed()))
                .check(matches(isDisplayed()))
            onView(withId(R.id.search_input))
                .perform(replaceText("nothing matches"), closeSoftKeyboard())
            onView(allOf(withId(R.id.empty_title), isDisplayed()))
                .check(matches(withText(R.string.empty_search)))
        }
    }

    @Test
    fun longLocationKeepsTimetableUsableInLandscapeWithKeyboard() {
        seed(room = List(25) { "Hall" }.joinToString("\n"))
        ActivityScenario.launch(HomepageActivity::class.java).use { scenario ->
            waitFor {
                var ready = false
                scenario.onActivity {
                    ready =
                        it.findViewById<android.widget.TextView>(R.id.next_course_title)
                            .text
                            .isNotBlank()
                }
                ready
            }
            scenario.onActivity {
                it.requestedOrientation =
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            waitFor {
                var landscape = false
                scenario.onActivity {
                    landscape =
                        it.resources.configuration.orientation ==
                            android.content.res.Configuration.ORIENTATION_LANDSCAPE
                }
                landscape
            }
            onView(withId(R.id.search_input)).check(matches(isDisplayed()))
            scenario.onActivity { a ->
                assertTrue(
                    "Landscape must leave room for timetable rows",
                    a.findViewById<View>(R.id.pager).height >=
                        64 * a.resources.displayMetrics.density,
                )
            }
            onView(withId(R.id.search_input)).perform(click())
            waitFor {
                var visible = false
                scenario.onActivity {
                    visible =
                        androidx.core.view.ViewCompat.getRootWindowInsets(
                                it.findViewById(R.id.root),
                            )
                            ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
                }
                visible
            }
            onView(withId(R.id.search_input)).check(matches(isDisplayed()))
            scenario.onActivity {
                assertTrue(
                    "Search keyboard must not collapse the list",
                    it.findViewById<View>(R.id.pager).height > 0,
                )
            }
            androidx.test.espresso.Espresso.closeSoftKeyboard()
        }
    }

    @Test
    fun notificationEntryStillOffersDeleteUndo() {
        val id = seed()
        val intent =
            android.content.Intent(app, HomepageActivity::class.java).putExtra("open_course_id", id)
        ActivityScenario.launch<HomepageActivity>(intent).use {
            waitFor {
                var ready = false
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync {
                        ready =
                            androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                                .getInstance()
                                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                                .any { it is com.ams.timesyncedualertv2.ui.CourseDetailsActivity }
                    }
                ready
            }
            onView(withId(R.id.button_delete_course)).perform(reveal(), click())
            onView(withText(R.string.delete)).inRoot(isDialog()).perform(click())
            waitFor { count() == 0 }
            onView(withText(R.string.undo)).perform(click())
            waitFor { count() == 1 }
        }
    }
}
