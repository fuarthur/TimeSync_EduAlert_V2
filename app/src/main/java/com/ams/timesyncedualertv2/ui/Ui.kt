package com.ams.timesyncedualertv2.ui

import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.data.CourseConflictException
import com.ams.timesyncedualertv2.data.CourseMissingException
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

object Ui {
    val dayNames=intArrayOf(R.string.monday_short,R.string.tuesday_short,R.string.wednesday_short,R.string.thursday_short,R.string.friday_short,R.string.saturday_short,R.string.sunday_short)
    fun setup(a:AppCompatActivity,back:(()->Unit)?=null) {
        val root=a.findViewById<View>(R.id.root)
        WindowCompat.setDecorFitsSystemWindows(a.window,false)
        val dark=(a.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(a.window,root).apply { isAppearanceLightStatusBars=!dark; isAppearanceLightNavigationBars=!dark }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view,insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime=insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left,bars.top,bars.right,maxOf(bars.bottom,ime.bottom))
            insets
        }
        ViewCompat.requestApplyInsets(root)
        val content=a.findViewById<View>(R.id.main_content)
        root.addOnLayoutChangeListener { v,_,_,_,_,_,_,_,_ ->
            val max=(720*a.resources.displayMetrics.density).toInt()
            val width=minOf(v.width-v.paddingLeft-v.paddingRight,max)
            if(width>0 && content.layoutParams.width!=width) content.layoutParams=content.layoutParams.apply { this.width=width }
        }
        if(back!=null) a.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { back() }
    }
    fun message(a:AppCompatActivity,text:Int)=Snackbar.make(a.findViewById(R.id.root),text,Snackbar.LENGTH_LONG).show()
    fun error(a:AppCompatActivity,error:Throwable) {
        val message=when(error) {
            is CourseConflictException -> a.getString(R.string.conflict,error.courseName)
            is CourseMissingException -> a.getString(R.string.missing_course)
            else -> a.getString(R.string.operation_failed)
        }
        Snackbar.make(a.findViewById(R.id.root),message,Snackbar.LENGTH_LONG).show()
    }
    fun onColor(color:Int):Int {
        val luminance=androidx.core.graphics.ColorUtils.calculateLuminance(color or Color.BLACK)
        return if(luminance>0.45) Color.BLACK else Color.WHITE
    }
}
