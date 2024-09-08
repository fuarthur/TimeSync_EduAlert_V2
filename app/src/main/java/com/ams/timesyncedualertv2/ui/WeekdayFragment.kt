package com.ams.timesyncedualertv2.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.model.Course
import com.ams.timesyncedualertv2.model.CourseAdapter
import java.time.LocalTime

class WeekdayFragment : Fragment() {

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
        // 绑定 Fragment 的布局文件
        val view = inflater.inflate(R.layout.fragment_weekday, container, false)

        // 获取传递的 weekday 参数
        val weekday = arguments?.getString(ARG_WEEKDAY)

        // 获取 RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.courses_recycler_view)

        // 设置 LayoutManager
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 设置 Adapter，获取相应 weekday 的课程数据
        val courses = getCoursesForWeekday(weekday)
        recyclerView.adapter = CourseAdapter(courses)

        return view
    }

    // 根据 weekday 获取对应的课程数据
    private fun getCoursesForWeekday(weekday: String?): List<Course> {
        // TODO: 未来通过数据库或用户输入的方式来实现
        return emptyList()
    }
}
