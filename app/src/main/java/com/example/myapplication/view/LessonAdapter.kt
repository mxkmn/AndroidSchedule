package com.example.myapplication.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemLessonBinding
import com.example.myapplication.datamanager.Lesson

class LessonAdapter(var lessons: List<Lesson>): RecyclerView.Adapter<LessonAdapter.TodoViewHolder>() {
  inner class TodoViewHolder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    val binding = ItemLessonBinding.inflate(layoutInflater, parent, false)

    return TodoViewHolder(binding)
  }

  override fun getItemCount(): Int {
    return lessons.size
  }

  override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
    val lesson = lessons[position]
    holder.itemView.apply {
      var place = ""
      place += getAddString(place, lesson.strDayOfWeek)
      place += getAddString(place, lesson.strTime, 2)
      place += getAddString(place, lesson.classroomName, 1)
      if (lesson.subgroup != 0) {
        place += getAddString(place, "подгруппа ${lesson.subgroup}", 1)
      }
      holder.binding.place.text = place

      holder.binding.name.text = lesson.name

      var meta = ""
      meta += getAddString(meta, lesson.type)
      meta += getAddString(meta, lesson.teacherName, 2)
      holder.binding.meta.text = meta

//      val internalWeek: Int, // внутренний номер недели
//      val dayOfWeek: Int, // день недели, начало с 0
//      val classroomLink: String = "", // ссылка на аудиторию
//      val teacherLink: String = "", // ссылка на препода

//      todoList.add(Todo(binding.etLesson.text.toString(), false))
//      adapter.notifyItemChanged(todoList.size - 1)
    }
  }

  private fun getAddString(string: String, newText: String, separatorType: Int = 0): String {
    val separator = when (separatorType) {
      1 -> " | "
      2 -> ", "
      else -> ""
    }
    return when {
      newText.isBlank() -> ""
      string.isBlank() -> newText.replaceFirstChar { it.uppercase() }
      else -> "$separator$newText"
    }
  }
}