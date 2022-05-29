package com.example.myapplication.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemLessonBinding
import com.example.myapplication.datamanager.Lesson

class LessonAdapter(private val lessons: List<Lesson>) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {
  inner class LessonViewHolder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    val binding = ItemLessonBinding.inflate(layoutInflater, parent, false)

    return LessonViewHolder(binding)
  }

  override fun getItemCount(): Int {
    return lessons.size
  }

  override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
    val lesson = lessons[position]
    holder.itemView.apply {
      var place = ""
      place += getConcatString(place, lesson.strTime)
      place += getConcatString(place, lesson.classroomName, 1)
      if (lesson.subgroup != 0) {
        place += getConcatString(place, "подгруппа ${lesson.subgroup}", 1)
      }
      holder.binding.place.text = place

      holder.binding.name.text = lesson.name

      var meta = ""
      meta += getConcatString(meta, lesson.type)
      meta += getConcatString(meta, lesson.teacherName, 2)
      holder.binding.meta.text = meta

//      val classroomLink: String = "", // ссылка на аудиторию
//      val teacherLink: String = "", // ссылка на препода

      // добавить что-то без обновления вообще всего:
//      todoList.add(Todo(binding.etLesson.text.toString(), false))
//      adapter.notifyItemChanged(todoList.size - 1)
    }
  }

  private fun getConcatString(string: String, newText: String, separatorType: Int = 0): String {
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