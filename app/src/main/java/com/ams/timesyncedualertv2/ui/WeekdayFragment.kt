package com.ams.timesyncedualertv2.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.domain.Timetable
import com.ams.timesyncedualertv2.model.CourseAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WeekdayFragment : Fragment() {
    companion object {
        fun newInstance(weekday: Int) =
            WeekdayFragment().apply {
                require(weekday in 1..7)
                arguments = Bundle().apply { putInt("weekday", weekday) }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_weekday, parent, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.courses_recycler_view)
        val empty = view.findViewById<View>(R.id.empty_state)
        val title = view.findViewById<TextView>(R.id.empty_title)
        val hint = view.findViewById<TextView>(R.id.empty_hint)
        list.layoutManager = LinearLayoutManager(requireContext())
        ViewCompat.setOnApplyWindowInsetsListener(list) { target, insets ->
            val bottom = if (insets.isVisible(WindowInsetsCompat.Type.ime())) 16 else 104
            target.setPadding(
                target.paddingLeft,
                target.paddingTop,
                target.paddingRight,
                (bottom * resources.displayMetrics.density).toInt(),
            )
            insets
        }
        ViewCompat.requestApplyInsets(list)
        view.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val compact = (bottom - top) / resources.displayMetrics.density < 200
            hint.visibility = if (compact) View.GONE else View.VISIBLE
            val padding = ((if (compact) 12 else 32) * resources.displayMetrics.density).toInt()
            empty.setPadding(padding, padding, padding, padding)
        }
        empty.visibility = View.VISIBLE
        title.setText(R.string.loading)
        hint.text = ""
        val day = requireArguments().getInt("weekday")
        val home = requireActivity() as HomepageActivity
        val adapter = CourseAdapter(requireContext(), emptyList()) { home.openCourse(it) }
        list.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    combine(
                            (requireActivity().application as TimeSyncApplication)
                                .repository
                                .courses,
                            home.searchQuery,
                        ) { all, query ->
                            Timetable.onDay(all, day, query)
                        }
                        .collect { courses ->
                            adapter.updateCourses(courses)
                            empty.visibility = if (courses.isEmpty()) View.VISIBLE else View.GONE
                            val searching = home.searchQuery.value.isNotBlank()
                            title.setText(
                                if (searching) R.string.empty_search else R.string.empty_day,
                            )
                            hint.setText(
                                if (searching) R.string.empty_search_hint
                                else R.string.empty_day_hint,
                            )
                        }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    empty.visibility = View.VISIBLE
                    title.setText(R.string.load_failed)
                    hint.setText(R.string.retry)
                    empty.setOnClickListener { requireActivity().recreate() }
                }
            }
        }
    }
}
