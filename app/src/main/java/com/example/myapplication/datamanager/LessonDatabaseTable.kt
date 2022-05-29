package com.example.myapplication.datamanager

import android.util.Log
import androidx.room.*

@Entity(tableName = "lesson_table")
data class Lesson(
  val internalWeek: Int, // внутренний номер недели
  val dayOfWeek: Int, // день недели, начало с 0
  val time: Int, // в минутах, 8:15 == 495
  val name: String = "", // название предмета
  val type: String = "", // тип пары, лекция/лаба/etc.
  val subgroup: Int = -1, // подгруппа, 0 == общая
  val classroomName: String = "", // аудитория
  val classroomLink: String = "", // ссылка на аудиторию
  val teacherName: String = "", // ФИО препода
  val teacherLink: String = "", // ссылка на препода

  @PrimaryKey(autoGenerate = true) val id: Int = 0
                 ) {
  val strTime: String get() {
    val minute = time % 60
    return "${time/60}:${if (minute < 10) "0" else ""}$minute"
  }
  val strDayOfWeek: String get() = when(dayOfWeek) {
    0 -> "понедельник"
    1 -> "вторник"
    2 -> "среда"
    3 -> "четверг"
    4 -> "пятница"
    5 -> "суббота"
    6 -> "воскресенье"
    else -> "неизвестно"
  }
  fun log() {
    Log.i("mxkmnLesson", "week $internalWeek: $strDayOfWeek, $strTime, $name, $type, подгруппа $subgroup, аудитория $classroomName, $classroomLink, преподаватель $teacherName, $teacherLink.")
  }
}

@Dao
interface LessonDao {
  @Insert
  fun insert(lesson: Lesson)
  @Insert
  fun insert(lessons: List<Lesson>)

  @Update
  fun update(lesson: Lesson)
  @Update
  fun update(lessons: List<Lesson>)

  @Delete
  fun delete(lesson: Lesson)
  @Delete
  fun delete(lessons: List<Lesson>)

  @Query("SELECT * FROM lesson_table ORDER BY internalWeek, dayOfWeek, time, subgroup")
  fun getAllSorted(): List<Lesson>

  @Query("SELECT * FROM lesson_table")
  fun getAll(): List<Lesson>

  @Query("SELECT * FROM lesson_table WHERE internalWeek < :internalCurrentWeek")
  fun getOutdatedLessons(internalCurrentWeek: Int): List<Lesson>

  @Query("SELECT * FROM lesson_table WHERE dayOfWeek = :dayOfWeek")
  fun getByDay(dayOfWeek: Int): List<Lesson>

  @Query("SELECT * FROM lesson_table WHERE internalWeek = :internalWeek")
  fun getByInternalWeek(internalWeek: Int): List<Lesson>

  @Query("SELECT * FROM lesson_table WHERE internalWeek = :internalWeek ORDER BY dayOfWeek, time, subgroup")
  fun getByInternalWeekSorted(internalWeek: Int): List<Lesson>
}