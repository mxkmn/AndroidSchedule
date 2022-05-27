package com.example.myapplication.view

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.logic.AutoUpdater
import com.example.myapplication.logic.Time
import com.example.myapplication.datamanager.DataManager
import com.example.myapplication.datamanager.StoredDatabase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var storage: DataManager
  private var selectedWeek = Time.getCurrentInternalWeek()

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
//      R.id.btnSettings -> {
//
//      }
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


  private fun initStorage() { // инициализация хранилища
    storage = DataManager(applicationContext)

    lifecycleScope.launch {
      storage.isDbInitialized.collect { success -> // connection listener
        if (success != null) {
          if (success) { // успешное подключение
            displayAll()
//            Toast.makeText(applicationContext, "Подключено к БД!", Toast.LENGTH_LONG).show()
//            Log.i("mxkmnInitStorage", "DB connected")
          } else { // ошибка при подключении
            Toast.makeText(applicationContext, "При подключении БД возникла критическая ошибка!", Toast.LENGTH_LONG).show()
            Log.e("mxkmnInitStorage", "DB not connected")
          }
        }
      }
    }

    lifecycleScope.launch {
      storage.flowParseResult.collect { parseResult -> // connection listener
        if (parseResult.week == selectedWeek) {
          if (parseResult.resultType != StoredDatabase.ParseResultType.NO_NEW_DATA) {
            displayAll()
          }
//          when (parseResult.resultType) {
//            StoredDatabase.ParseResultType.NEW_DATA -> {
//              displayAll()
//            }
//            StoredDatabase.ParseResultType.NO_NEW_DATA -> {
//              // do nothing
//            }
//            StoredDatabase.ParseResultType.NO_DATA -> {
//              displayAll()
//            }
//            StoredDatabase.ParseResultType.ERROR -> {
//              displayAll()
//            }
//          }
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

    storage.printWeekOnRecyclerView(selectedWeek, binding, applicationContext, this)
    supportActionBar?.title = Time.dateRange(selectedWeek)
  }
}