package com.example.myapplication.datamanager

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.myapplication.logic.Time
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread


class DataManager(private val context: Context) {
  val vars = StoredVariables(context)

  val isDbInitialized = MutableStateFlow<Boolean?>(null)

  var db: StoredDatabase? = null
    private set(value) {
      field = value

      isDbInitialized.value = true // отправка успешности инициализации БД в listener
    }
  init {
    try {
      synchronized(this) {
        db = Room.databaseBuilder(context, StoredDatabase::class.java, "lesson_and_html.db").build()
      }
    } catch (e: Exception) {
      Log.e("mxkmnDatabaseError", e.toString())
      isDbInitialized.value = false
    }
  }

  fun updateAndDeleteOutdatedWeek() {
    defaultUpdate()
    runBlocking {
      val needDeleteOutdatedWeeks = vars.getCurrentWeek() != Time.getCurrentInternalWeek()

      if (needDeleteOutdatedWeeks) {
        thread {
          val outdatedLessons = db!!.lessonDao().getOutdatedLessons(Time.getCurrentInternalWeek())
          if (outdatedLessons.isNotEmpty()) {
            db!!.lessonDao().delete(outdatedLessons)
            Log.i("mxkmnDeleteOutdated", "${outdatedLessons.size} lessons deleted!")
          }
          else {
            Log.i("mxkmnDeleteOutdated", "lessons not deleted (there are no outdated lessons)")
          }
        }
        vars.setCurrentWeek(Time.getCurrentInternalWeek())
      }
      else {
        Log.i("mxkmnDeleteOutdated", "lessons not deleted (there are no outdated weeks)")
      }
    }
  }

  private fun defaultUpdate(link: String = vars.fastLink) {
    val currentInternalWeek = Time.getCurrentInternalWeek()
    for (i in arrayOf(0, 1, -1, 2, -2)) {
      thread { // необходимо запускать в thread - иначе проблема соединения с сетью
        db!!.insertWeekFromWeb(currentInternalWeek + i, link)
      }
    }
  }

  fun setSubgroup(subgroup: Int) {
    runBlocking {
        vars.setSubgroup(subgroup)
    }
  }

  fun setLinkAndUpdate(correctLink: String) {
    runBlocking {
      vars.setLink(correctLink) // link is a string without protocol and parameters (after '&')
    }

    db!!.clearAllTables() // необходимо запускать в thread - иначе вылет
    defaultUpdate(correctLink)
  }
}