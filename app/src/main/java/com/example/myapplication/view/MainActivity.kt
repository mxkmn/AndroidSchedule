package com.example.myapplication.view

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.SettingsDialogBinding
import com.example.myapplication.datamanager.DataManager
import com.example.myapplication.datamanager.Lesson
import com.example.myapplication.datamanager.StoredDatabase
import com.example.myapplication.logic.AutoUpdater
import com.example.myapplication.logic.CalendarWorker
import com.example.myapplication.logic.Time
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.time.LocalDate
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var storage: DataManager
  private var selectedWeek = Time.getCurrentInternalWeek()
  private lateinit var calendar: CalendarWorker
  private var dbUpdated = false

  override fun onCreate(savedInstanceState: Bundle?) { // при запуске приложения
    super.onCreate(savedInstanceState) // вызываем стандартную инициализацию через родителя

    initStorage() // инициализация хранилища - там же будет и первая отрисовка
    AutoUpdater.updateAfterSomeMinutes(applicationContext, 12 * 60) // подключение автообновлений каждые 12 часов
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean { // добавляем кнопки в ActionBar
    menuInflater.inflate(R.menu.main_action_bar, menu)
    return true
  }
  override fun onOptionsItemSelected(item: MenuItem): Boolean { // при нажатии на кнопку в ActionBar
    when (item.itemId) {
      R.id.btnSettings -> { // создание диалогового окна с настройками
        showSettingsDialog()
      }
      R.id.btnPrevWeek -> {
        selectedWeek--
        displayAll()
      }
      R.id.btnNextWeek -> {
        selectedWeek++
        displayAll()
      }
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }


  override fun onConfigurationChanged(newConfig: Configuration) { // при изменении конфигурации экрана (установка темы)
    super.onConfigurationChanged(newConfig)

//    val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
//    displayAll(nightModeFlags == Configuration.UI_MODE_NIGHT_YES)
  }

  private fun updateCalendar() { // обновление календаря
    dbUpdated = false
    thread { calendar.addSchedule(storage.db!!.lessonDao().getAll()) }
  }
  private fun initStorage() { // инициализация хранилища
    storage = DataManager(this)

    lifecycleScope.launch {
      storage.isDbInitialized.collect { success -> // connection listener
        if (success != null) {
          if (success) { // успешное подключение
            Log.i("mxkmnInitStorage", "DB connected")
            initCalendar()

            displayAll()

            storage.db!!.flowParseResult.collect { parseResult ->
              dbUpdated = true
              if (calendar.isAccessible) {
                updateCalendar()
              }

              saveResult(parseResult)
              if (parseResult.week == selectedWeek) {
                if (parseResult.resultType != StoredDatabase.ParseResultType.NO_NEW_DATA) {
                  displayAll() // отобразить, если что-то изменилось
                }
              }
            }
          } else { // ошибка при подключении
            Toast.makeText(applicationContext, "При подключении БД возникла критическая ошибка!", Toast.LENGTH_LONG).show()
            Log.e("mxkmnInitStorage", "DB not connected")
          }
        }
      }
    }
  }

  private fun initCalendar() {
    val splittedLink = storage.vars.fastLink.split("?")
    val correctLink = splittedLink[0]
    calendar = CalendarWorker(contentResolver, this, correctLink)

    lifecycleScope.launch {
      calendar.flowAccessibleNow.collect {
        if (dbUpdated) {
          updateCalendar()
        }
      }
    }
  }

  private fun displayAll(setNightTheme: Boolean? = null) { // отрисовка всего на форме
    if (setNightTheme != null) {
      setTheme(if (setNightTheme) R.style.Theme_MyApplicationNight else R.style.Theme_MyApplication)
      Log.i("mxkmnTheme", "$setNightTheme")
    }
    if (setNightTheme != null || !this::binding.isInitialized) {
      binding = ActivityMainBinding.inflate(layoutInflater)
      setContentView(binding.root)
    }

    if (storage.db != null) { // только когда БД подключена, иначе вылет
      showSelectedWeek()
    }
    supportActionBar?.title = Time.dateRange(selectedWeek)
  }

  private fun showSelectedWeek() {
    thread {
      val weekLessons = storage.db!!.lessonDao().getByInternalWeekSorted(selectedWeek)
      runOnUiThread {
        val rvDays = binding.rvDays
        val txtInfo = binding.txtInfo
        if (weekLessons.isNotEmpty()) {
          if (showDaysWithSubgroupCheck(weekLessons)) { // true когда есть занятия, false когда надо вывести txt
            txtInfo.visibility = View.GONE
            rvDays.visibility = View.VISIBLE
          } else {
            txtInfo.text = getString(R.string.no_data_for_subgroup)
            txtInfo.visibility = View.VISIBLE
            rvDays.visibility = View.GONE
          }
        } else {
          when (getWeekParseResultType(selectedWeek)) {
            StoredDatabase.ParseResultType.NO_DATA -> txtInfo.setText(R.string.no_data)
            StoredDatabase.ParseResultType.ERROR -> txtInfo.setText(R.string.error)
            else -> { // внутри нет полезных данных (что-то о наличии данных или null) -> эту неделю нужно запросить
              txtInfo.setText(R.string.getting_data)
              thread {
                storage.db!!.insertWeekFromWeb(selectedWeek, storage.vars.fastLink)
              }
            }
          }
          txtInfo.visibility = View.VISIBLE
          rvDays.visibility = View.GONE
        }
      }
    }
  }

  private fun showDaysWithSubgroupCheck(weekLessons: List<Lesson>): Boolean {
    var lessonsAtDay = mutableListOf<Lesson>()
    val days = mutableListOf<List<Lesson>>()
    var day = 0
    val subgroup = storage.vars.fastSubgroup
    weekLessons.forEach {
      if (it.dayOfWeek != day) {
        if (lessonsAtDay.isNotEmpty()) {
          days.add(lessonsAtDay)
        }
        lessonsAtDay = mutableListOf()
        day = it.dayOfWeek
      }

      // если не выбрана подгруппа или (для всех подгрупп или подгруппа совпадает):
      if (subgroup == 0 || (it.subgroup == 0 || it.subgroup == subgroup)) {
        lessonsAtDay.add(it)
      }
    }
    if (lessonsAtDay.isNotEmpty()) {
      days.add(lessonsAtDay)
    }

    if (days.isEmpty()) {
      return false
    }

    val rvDays = binding.rvDays
    rvDays.adapter = WeekAdapter(days)
    rvDays.layoutManager = LinearLayoutManager(applicationContext)
    return true
  }

  private fun showSettingsDialog() {
    val alertDialog = AlertDialog.Builder(this).create()
    val settingsBinding = SettingsDialogBinding.inflate(layoutInflater) // SettingsDialogBinding provided by View binding
    alertDialog.setView(settingsBinding.root)

    settingsBinding.etLink.setText(storage.vars.fastLink)
    thread {
      val subgroups = mutableListOf<Int>()
      for (i in storage.db!!.lessonDao().getAllSorted()) {
        if (i.subgroup != 0 && !subgroups.contains(i.subgroup)) {
          subgroups.add(i.subgroup)
        }
      }
      val subgroupsStr = mutableListOf<String>()
      subgroupsStr.add(getString(R.string.subgroups_all))
      for (i in subgroups) {
        subgroupsStr.add(getString(R.string.subgroups_indexed, i))
      }

      val select = storage.vars.fastSubgroup
      runOnUiThread {
        settingsBinding.spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subgroupsStr)
        settingsBinding.spinner.setSelection(if (select > subgroupsStr.size-1) 0 else select)
      }
    }

    settingsBinding.btnSave.setOnClickListener {
      trySetUri(settingsBinding.etLink.text.toString())
      val subgroup = settingsBinding.spinner.selectedItemId.toInt()
      if (subgroup != storage.vars.fastSubgroup) {
        storage.setSubgroup(subgroup)
        showSelectedWeek()
      }

      alertDialog.dismiss()
    }
    settingsBinding.btnCancel.setOnClickListener {
      alertDialog.dismiss() // close the dialog
    }

    alertDialog.show()
  }

  private fun trySetUri(uri: String) {
    thread {
      try {
        val splitted = uri.trim().split("://")
        if (splitted.size < 3 && splitted.isNotEmpty()) {
          val uriWithoutProtocol = splitted[splitted.size-1]
          val splittedUriWithoutProtocol = uriWithoutProtocol.split("&")
          val correctLink = splittedUriWithoutProtocol[0]

          if (correctLink == storage.vars.fastLink) // а вдруг введена та же ссылка?
            return@thread // выходим из функции без дальнейших действий

          val date = LocalDate.now()
          Jsoup.connect("https://$correctLink&date=${date.year}-${date.month.value}-${date.dayOfMonth}").get() // проверка возможности подключения к сайту

          listWithCheckedWeeks.clear()
          storage.setLinkAndUpdate(correctLink)
        }
        else {
          runOnUiThread {
            Toast.makeText(applicationContext, "Ошибка при задании группы (ссылку невозможно распознать). Скопируй ссылку на расписание из браузера", Toast.LENGTH_LONG).show()
          }
        }
      } catch (e: Exception) {
        runOnUiThread {
          Toast.makeText(applicationContext, "Ошибка при задании группы (подключение к сайту невозможно). Скопируй правильную ссылку на группу из браузера или проверь подключение к интернету", Toast.LENGTH_LONG).show()
        }
        Log.e("mxkmnSetUriException", e.toString())
      }
    }
  }

  private val listWithCheckedWeeks = mutableListOf<StoredDatabase.ParseResult>()
  private fun getWeekParseResultType(week: Int): StoredDatabase.ParseResultType? {
    return listWithCheckedWeeks.firstOrNull { it.week == week }?.resultType
  }
  private fun saveResult(parseResult: StoredDatabase.ParseResult) {
    listWithCheckedWeeks.removeIf { it.week == parseResult.week }
    if (parseResult.resultType != StoredDatabase.ParseResultType.NEW_DATA && parseResult.resultType != StoredDatabase.ParseResultType.NO_NEW_DATA)
      listWithCheckedWeeks.add(parseResult)
  }
}