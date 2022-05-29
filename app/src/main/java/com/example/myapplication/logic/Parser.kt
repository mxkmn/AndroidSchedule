package com.example.myapplication.logic

import android.util.Log
import com.example.myapplication.datamanager.Lesson
import org.jsoup.Jsoup
import org.jsoup.select.Elements

enum class ScheduleTypes { ODD, EVEN }

data class ScheduleHtmlData (
  val data: Elements,
  val type: ScheduleTypes,
  val internalWeek: Int
)

class Parser {
  companion object {
    private fun getCoolString(notCool: String): String {
      return notCool.trim().replaceFirstChar{ it.uppercase() }
    }

    fun getScheduleHtmlData(uri: String, internalWeek: Int): ScheduleHtmlData? {
      try {
        val doc = Jsoup.connect("$uri").get()
        val oddScheduleContent = doc.getElementsByAttributeValue("class", "full-odd-week")
        val evenScheduleContent = doc.getElementsByAttributeValue("class", "full-even-week")
        if (oddScheduleContent.size != 0) {
          return ScheduleHtmlData(oddScheduleContent, ScheduleTypes.ODD, internalWeek)
        }
        else if (evenScheduleContent.size != 0) {
          return ScheduleHtmlData(evenScheduleContent, ScheduleTypes.EVEN, internalWeek)
        }
        else {
          Log.w("mxkmnGetScheduleHtmlData", "No schedule info ($uri)")
          return ScheduleHtmlData(oddScheduleContent, ScheduleTypes.ODD, internalWeek)
        }
      } catch (e: Exception) {
        Log.e("mxkmnGetScheduleHtmlData", e.toString())
        return null
      }
    }

    fun getLessons(scheduleHtmlData: ScheduleHtmlData): ArrayList<Lesson> {
      val arrayOfLessons: ArrayList<Lesson> = arrayListOf<Lesson>()
      var dayOfWeek: Int = -1

      for (element in scheduleHtmlData.data[0].children()) { // scheduleContent[0] - по идее единственный full-even|odd-week
        if (element.className() == "day-heading") {
          dayOfWeek = when(element.text().lowercase().split(",")[0]) {
            "понедельник" -> 0
            "вторник" -> 1
            "среда" -> 2
            "четверг" -> 3
            "пятница" -> 4
            "суббота" -> 5
            "воскресенье" -> 6
            else -> -1
          }
        }
        else if ((element.className() == "class-lines") && (dayOfWeek != -1)) {
          for (timeAndLessons in element.children()) { // мы находимся в class-line-item, который содержит множество карточек с временем и парами для этого времени
            var time = -1 // class-time
            for (infoTiles in timeAndLessons.child(0).children()) { // переходим в карточку время + пара[], каждую пару нужно сохранить
              var name = "" // class-pred
              var type = "" // class-info with prep in href
              var subgroup = 0 // class-info with group in href
              var teacherName = "" // class-info a text
              var teacherLink = "" // class-info a href attribute
              var classroomName = "" // class-aud a text
              var classroomLink = "" // class-aud a href attribute

              if (infoTiles.className() == "class-time") { // если карточка времени
                val timeStr = infoTiles.text()
                time = timeStr.substring(0, timeStr.indexOf(":")).toInt()*60 + timeStr.substring(timeStr.indexOf(":")+1).toInt()
//                Log.i("mxkmnTime", time.toString())
//                    Log.i("mxkmnTime", timeStr.substring(timeStr.indexOf(':'))
              } // или если карточка пары нужной недели:
              else if ((time != -1) && ((infoTiles.className() == "class-tail class-all-week") ||
                    (infoTiles.className() == (if (scheduleHtmlData.type == ScheduleTypes.EVEN) "class-tail class-even-week" else "class-tail class-odd-week")))) {
                for (info in infoTiles.children()) {
                  if (info.className() == "class-pred") {
                    name = getCoolString(info.text())
//                    Log.i("mxkmnName", name)
                  }
                  else if (info.className() == "class-aud") {
                    if (info.children().size == 1) {
                      val tempA = info.child(0)
                      classroomName = tempA.ownText()
                      classroomLink = tempA.attr("href")
                    }
                  }
                  else if (info.className() == "class-info") {
                    if ((info.children().size > 0) && (info.child(0).attributes().toString().contains("group"))) {
                      val tempHtml = info.html()
                      val subgroupStr = if ("</a>" in tempHtml) tempHtml.substring(tempHtml.lastIndexOf("</a>")+4) else tempHtml
                      subgroup = if ("подгруппа " in subgroupStr) subgroupStr.substring(subgroupStr.lastIndexOf("подгруппа ")+10).toInt() else 0
                    }
                    else {
                      val tempHtml = info.html()
                      type = getCoolString(if ("<a" in tempHtml) tempHtml.substring(0, tempHtml.indexOf("<a")) else tempHtml)

                      if (info.children().size == 1) {
                        val tempA = info.child(0)
                        teacherName = tempA.ownText()
                        teacherLink = tempA.attr("href")
                      }
                    }
                  }
                }
                if (name != "") {
                  arrayOfLessons.add(Lesson(scheduleHtmlData.internalWeek, dayOfWeek, time, name, type, subgroup, classroomName, classroomLink, teacherName, teacherLink))
                }
              }
            }
          }
        }
      }
      return arrayOfLessons
    }
  }
}