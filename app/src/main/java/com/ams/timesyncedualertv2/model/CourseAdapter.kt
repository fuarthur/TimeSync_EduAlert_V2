package com.ams.timesyncedualertv2.model

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.ui.CourseDetailsActivity

class CourseAdapter(private val context:Context,courses:List<CourseEntity>,private val onOpen:((CourseEntity)->Unit)?=null):RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {
    private val sortedCourses=courses.sortedWith(compareBy<CourseEntity> { it.startTime }.thenBy { it.id })
    class CourseViewHolder(view:View):RecyclerView.ViewHolder(view) {
        val nameTextView:TextView=view.findViewById(R.id.textView_course_name)
        val locationTextView:TextView=view.findViewById(R.id.textView_course_location)
        val timeTextView:TextView=view.findViewById(R.id.textView_course_time)
        val dot:View=view.findViewById(R.id.course_color_dot)
    }
    override fun onCreateViewHolder(parent:ViewGroup,type:Int)=CourseViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_course,parent,false))
    override fun getItemCount()=sortedCourses.size
    override fun onBindViewHolder(holder:CourseViewHolder,position:Int) {
        val c=sortedCourses[position]
        holder.nameTextView.text=c.name
        holder.locationTextView.text=c.location.ifBlank { context.getString(R.string.no_location) }
        holder.timeTextView.text=context.getString(R.string.course_time_format,c.startTime,c.endTime)
        holder.dot.background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(c.color) }
        holder.itemView.setOnClickListener {
            if(onOpen!=null) onOpen.invoke(c) else context.startActivity(Intent(context,CourseDetailsActivity::class.java).putExtra("course_id",c.id))
        }
    }
}
