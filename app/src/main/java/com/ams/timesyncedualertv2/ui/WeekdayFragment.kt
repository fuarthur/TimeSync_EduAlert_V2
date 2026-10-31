package com.ams.timesyncedualertv2.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.db.CourseDao
import com.ams.timesyncedualertv2.model.CourseAdapter
import com.ams.timesyncedualertv2.model.CourseEntity
import kotlinx.coroutines.launch

class WeekdayFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var courseDao: CourseDao // 声明 courseDao

    companion object {
        private const val ARG_WEEKDAY = "weekday"

        // 创建 fragment 的工厂方法，通过参数生成不同的实例
        fun newInstance(weekday: String): WeekdayFragment {
            val fragment = WeekdayFragment()
            val args = Bundle()
            args.putString(ARG_WEEKDAY, weekday)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_weekday, container, false)

        // 初始化 RecyclerView
        recyclerView = view.findViewById(R.id.courses_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 获取数据库实例并初始化 courseDao
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        courseDao = db.courseDao() // 初始化 courseDao

        // 使用 viewLifecycleOwner.lifecycleScope 启动协程，保证协程在 Fragment 销毁时自动取消
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                courseDao.observeAll().collect { all ->
                    val day=mapWeekdayToInt(arguments?.getString(ARG_WEEKDAY) ?: "")
                    courseAdapter=CourseAdapter(requireContext(),all.filter { day in it.weekday })
                    recyclerView.adapter=courseAdapter
                }
            }
        }

        return view
    }

    private suspend fun getCoursesForWeekday(weekday: String): List<CourseEntity> {
        val weekdayInt = mapWeekdayToInt(weekday)
        Log.d("WeekdayFragment", "getCoursesForWeekday called with weekday: $weekday ($weekdayInt)")


        return if (weekdayInt != -1) {
            courseDao.getCoursesForWeekday(weekdayInt)
        } else {
            emptyList()
        }
    }

    private fun mapWeekdayToInt(weekday: String): Int {
        return when (weekday) {
            "Mon." -> 1
            "Tue." -> 2
            "Wed." -> 3
            "Thu." -> 4
            "Fri." -> 5
            else -> -1
        }
    }
}
