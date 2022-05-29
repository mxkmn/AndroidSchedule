package com.example.myapplication.logic

import com.example.myapplication.datamanager.Lesson
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.*

class Time {
  companion object {
    private val delayHours: Int = 2

    fun getLessonStartDateTime(lesson: Lesson): LocalDateTime {
      val weekOffset = lesson.internalWeek - getCurrentInternalWeek()
      val day = LocalDate.now().plusDays((weekOffset*7 - (getActualWeekDay()-1) + lesson.dayOfWeek).toLong())
      val time = LocalTime.of(lesson.time / 60, lesson.time % 60)

      return LocalDateTime.of( day, time )
    }
    private fun getActualWeekNum(): Int {
      return LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfYear())
    }
    fun getActualWeekDay(): Int { // воскресенье == 7
      return LocalDate.now().get(WeekFields.of(Locale.getDefault()).dayOfWeek())
    }
    fun getCurrentDateTimeString(): String {
      return LocalDateTime.now().toString()
    }
    fun getWaitHours(lastCheckDataTimeString: String): Int {
      return delayHours - Duration.between(LocalDateTime.parse(lastCheckDataTimeString), LocalDateTime.now()).toHours().toInt()
    }
    fun isDelayBetweenNowAndStringStillSmall(lastCheckDataTimeString: String): Boolean {
      return !(Duration.between(LocalDateTime.parse(lastCheckDataTimeString).plusHours(delayHours.toLong()), LocalDateTime.now()).isNegative)
    }
    fun dateRange(week: Int): String {
      val weekOffset = week - getCurrentInternalWeek()
      val startingDay = LocalDate.now().plusDays((weekOffset*7 - (getActualWeekDay()-1)).toLong())
      val endingDay = startingDay.plusDays(6)
      return "${startingDay.dayOfMonth}.${startingDay.monthValue} - ${endingDay.dayOfMonth}.${endingDay.monthValue}"
    }
    private fun getWeeksInYearsSince2022(year: Int): Int {
      if (year < 2023)
        return 0

      return when (year) {
        2023 -> 53
        2024 -> 53+54
        2025 -> 53+54+54
        2026 -> 53+54+54+54
        2027 -> 53+54+54+54+53
        2028 -> 53+54+54+54+53+53
        2029 -> 53+54+54+54+53+53+54
        2030 -> 53+54+54+54+53+53+54+54
        2031 -> 53+54+54+54+53+53+54+54+54
        2032 -> 53+54+54+54+53+53+54+54+54+54
        else -> getWeeksInYearsSince2022(year-1) +(LocalDate.of(year, 12, 31).get(WeekFields.of(Locale.getDefault()).weekOfYear()) + 1)
      }
    }
    fun getCurrentInternalWeek(): Int {
      return getActualWeekNum() + getWeeksInYearsSince2022(LocalDate.now().year)
    }
  }
}