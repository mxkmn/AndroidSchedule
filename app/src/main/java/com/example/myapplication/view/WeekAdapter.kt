package com.example.myapplication.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemWeekBinding
import com.example.myapplication.datamanager.Lesson
import com.example.myapplication.logic.Time
import java.time.LocalDate

class WeekAdapter(private val days: List<List<Lesson>>) : RecyclerView.Adapter<WeekAdapter.WeekViewHolder>() {
  inner class WeekViewHolder(val binding: ItemWeekBinding): RecyclerView.ViewHolder(binding.root)
private lateinit var con: Context
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    con = parent.context
    val binding = ItemWeekBinding.inflate(layoutInflater, parent, false)

    return WeekViewHolder(binding)
  }

  override fun getItemCount(): Int {
    return days.size
  }

  override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
    val day = days[position]

    val weekOffset = day[0].internalWeek - Time.getCurrentInternalWeek()
    val dayInfo = LocalDate.now().plusDays((weekOffset*7 - (Time.getActualWeekDay()-1) + day[0].dayOfWeek).toLong())

    holder.itemView.apply {
      holder.binding.txtDayName.text = "${day[0].strDayOfWeek} | ${dayInfo.dayOfMonth}.${dayInfo.monthValue}" // Resources.getSystem().getString(R.string.day_of_week, day[0].strDayOfWeek, dayInfo.dayOfMonth, dayInfo.monthValue)
      holder.binding.rvLessons.adapter = LessonAdapter(day)
      holder.binding.rvLessons.layoutManager = LinearLayoutManager(con)
    }
  }
}