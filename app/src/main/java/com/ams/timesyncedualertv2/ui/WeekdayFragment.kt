package com.ams.timesyncedualertv2.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.TimeSyncApplication
import com.ams.timesyncedualertv2.model.CourseAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class WeekdayFragment:Fragment() {
    companion object {
        fun newInstance(weekday:String)=WeekdayFragment().apply { arguments=Bundle().apply { putString("weekday",weekday) } }
    }
    override fun onCreateView(inflater:LayoutInflater,parent:ViewGroup?,state:Bundle?):View=inflater.inflate(R.layout.fragment_weekday,parent,false)
    override fun onViewCreated(view:View,state:Bundle?) {
        val list=view.findViewById<RecyclerView>(R.id.courses_recycler_view)
        val empty=view.findViewById<View>(R.id.empty_state)
        val title=view.findViewById<TextView>(R.id.empty_title)
        val hint=view.findViewById<TextView>(R.id.empty_hint)
        list.layoutManager=LinearLayoutManager(requireContext())
        empty.visibility=View.VISIBLE; title.setText(R.string.loading); hint.text=""
        val day=listOf("Mon.","Tue.","Wed.","Thu.","Fri.").indexOf(arguments?.getString("weekday"))+1
        viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                (requireActivity().application as TimeSyncApplication).repository.courses.collect { all ->
                    val courses=all.filter { day in it.weekday }
                    list.adapter=CourseAdapter(requireContext(),courses)
                    empty.visibility=if(courses.isEmpty()) View.VISIBLE else View.GONE
                    title.setText(R.string.empty_day); hint.setText(R.string.empty_day_hint)
                }
            } catch(e:Exception) {
                if(e is CancellationException) throw e
                empty.visibility=View.VISIBLE; title.setText(R.string.load_failed); hint.setText(R.string.retry)
                empty.setOnClickListener { requireActivity().recreate() }
            }
        } }
    }
}
