package com.example.myapplication.datamanager

import android.util.Log
import androidx.room.*
import com.example.myapplication.logic.Parser
import com.example.myapplication.logic.Time
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

@Database(entities = [Lesson::class], version = 1)
abstract class StoredDatabase: RoomDatabase() {
  abstract fun lessonDao(): LessonDao

  enum class ParseResultType {
    ERROR, NO_DATA, NO_NEW_DATA, NEW_DATA
  }
  class ParseResult(val week: Int, val resultType: ParseResultType)

  private val _parseResultEmitter = MutableSharedFlow<ParseResult>()
  val flowParseResult = _parseResultEmitter.asSharedFlow()

  fun insertWeekFromWeb(internalWeek: Int, link: String) {
    val currentInternalWeek: Int = Time.getCurrentInternalWeek()
    val plusDays = ((internalWeek-currentInternalWeek)*7).toLong()
    val date = LocalDate.now().plusDays(plusDays)

    val schedule = Parser.getScheduleHtmlData("https://$link&date=${date.year}-${date.month.value}-${date.dayOfMonth}", internalWeek)

    runBlocking {
      if (schedule == null)
        _parseResultEmitter.emit(ParseResult(internalWeek, ParseResultType.ERROR))
      else if (schedule.data.size == 0)
        _parseResultEmitter.emit(ParseResult(internalWeek, ParseResultType.NO_DATA))
      else
        _parseResultEmitter.emit(ParseResult(internalWeek, updateWeekData(Parser.getLessons(schedule), internalWeek)))
    }
  }

  private fun updateWeekData(data: ArrayList<Lesson>, internalWeek: Int): ParseResultType {
    val dataFromTable = lessonDao().getByInternalWeek(internalWeek).toMutableList()
    val arrayOfNewLessons: ArrayList<Lesson> = arrayListOf()
    for (lesson in data) { // поиск новых занятий
      val foundLesson = getSimilarLessonFromList(lesson, dataFromTable)
      if (foundLesson == null) {
        arrayOfNewLessons.add(lesson)
      }
      else {
        dataFromTable.remove(foundLesson)
      }
    }
    if (arrayOfNewLessons.size > 0 || dataFromTable.size > 0) {
      Log.i("mxkmnUpdateWeekData", "There are ${lessonDao().getByInternalWeek(internalWeek).size} lessons in DB at $internalWeek week. Removing ${dataFromTable.size} and inserting ${arrayOfNewLessons.size}...")
      lessonDao().delete(dataFromTable)
      lessonDao().insert(arrayOfNewLessons)
      Log.i("mxkmnUpdateWeekData", "There are ${lessonDao().getByInternalWeek(internalWeek).size} lessons in DB at $internalWeek week!")
      return ParseResultType.NEW_DATA
    } else {
      Log.i("mxkmnUpdateWeekData", "There are no new lessons in DB at $internalWeek week!")
      return ParseResultType.NO_NEW_DATA
    }
  }

  companion object {
    private fun getSimilarLessonFromList(lesson: Lesson, list: MutableList<Lesson>): Lesson? { // ищет аналогичную лекцию с отличающимся id
      for (i in list) {
        if (lesson.internalWeek == i.internalWeek && lesson.dayOfWeek == i.dayOfWeek && lesson.time == i.time &&
          lesson.name == i.name && lesson.type == i.type && lesson.subgroup == i.subgroup && lesson.classroomName == i.classroomName &&
          lesson.classroomLink == i.classroomLink && lesson.teacherName == i.teacherName && lesson.teacherLink == i.teacherLink ) {
          return i
        }
      }
      return null
    }
  }
}