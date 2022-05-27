package com.example.myapplication.datamanager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.logic.Time
import com.example.myapplication.view.LessonAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread


class DataManager(private val context: Context) {
  val vars = StoredVariables(context)

  val isDbInitialized = MutableStateFlow<Boolean?>(null)

  var db: StoredDatabase? = null
    private set(value) {
      field = value

      isDbInitialized.value = true // отправка успешности инициализации БД в listener'ы
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

  fun printWeekOnRecyclerView( currentWeek: Int, binding: ActivityMainBinding, context: Context, activity: Activity) {
    if (db == null) return

    thread {
      val week = db!!.lessonDao().getByInternalWeek(currentWeek)
      activity.runOnUiThread {
        val rvLessons = binding.rvLessons
        val txtNoData = binding.txtNoData
        val txtGettingData = binding.txtGettingData
        if (week.isNotEmpty()) {
          rvLessons.adapter = LessonAdapter(week)
          rvLessons.layoutManager = LinearLayoutManager(context)

          txtNoData.visibility = RecyclerView.GONE
          txtGettingData.visibility = RecyclerView.GONE
          rvLessons.visibility = RecyclerView.VISIBLE
        } else {
          if (isFirstUpdateAtCurrentAppLaunch(currentWeek)) {
            txtNoData.visibility = RecyclerView.GONE
            txtGettingData.visibility = RecyclerView.VISIBLE
          } else {
            txtNoData.visibility = RecyclerView.VISIBLE
            txtGettingData.visibility = RecyclerView.GONE
          }
          rvLessons.visibility = RecyclerView.GONE
        }
      }
    }
  }

  private val _parseResultEmitter = MutableSharedFlow<StoredDatabase.ParseResult>()
  val flowParseResult = _parseResultEmitter.asSharedFlow()

  private val arrayWithCheckedWeeks = mutableListOf<Int>()
  private fun isFirstUpdateAtCurrentAppLaunch(week: Int): Boolean {
    val isFirstUpdate = arrayWithCheckedWeeks.contains(week)
//    Log.i("mxkmnIsFirstUpdate", "week: $week, isFirstUpdate: $isFirstUpdate")
    if (isFirstUpdate)
      return false

    arrayWithCheckedWeeks.add(week)
    thread {
      val parseResult = db!!.insertWeekFromWeb(week)
      runBlocking {
        _parseResultEmitter.emit(parseResult)
      }
    }
    return true
  }

  fun updateIfNeeded() {
    val currentInternalWeek = Time.getCurrentInternalWeek()
    thread {
      for (i in 0..2) {
        db!!.insertWeekFromWeb(currentInternalWeek + i) // необходимо запускать в thread - иначе проблема соединения с сетью
      }
    }
    runBlocking {
      val needDeleteOutdatedWeeks = vars.getCurrentWeek() != Time.getCurrentInternalWeek()

      if (needDeleteOutdatedWeeks) {
        thread {
          val outdatedLessons = db!!.lessonDao().getOutdatedLessons(Time.getCurrentInternalWeek())
          if (outdatedLessons.isNotEmpty()) {
            db!!.lessonDao().delete(outdatedLessons)
            Log.i("mxkmnDeleteOutdatedWeek", "${outdatedLessons.size} lessons deleted!")
          }
          else {
            Log.i("mxkmnDeleteOutdatedWeek", "lessons not deleted (there are no outdated lessons)")
          }
        }
        vars.setCurrentWeek(Time.getCurrentInternalWeek())
      }
      else {
        Log.i("mxkmnDeleteOutdatedWeek", "lessons not deleted (there are no outdated weeks)")
      }
    }
  }
}