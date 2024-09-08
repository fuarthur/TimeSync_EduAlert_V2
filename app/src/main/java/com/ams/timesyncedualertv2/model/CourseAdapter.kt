package com.ams.timesyncedualertv2.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ams.timesyncedualertv2.R

class CourseAdapter(private val courses: List<Course>) :
    RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseName: TextView = view.findViewById(R.id.course_name)
        val courseTime: TextView = view.findViewById(R.id.course_time)
        val courseLocation: TextView = view.findViewById(R.id.course_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.courseName.text = course.name
        holder.courseTime.text = "${course.times.first} - ${course.times.second}"
        holder.courseLocation.text = course.location
        // 根据需要可以在这里设置背景色等 UI 特性
    }

    override fun getItemCount(): Int = courses.size
}
