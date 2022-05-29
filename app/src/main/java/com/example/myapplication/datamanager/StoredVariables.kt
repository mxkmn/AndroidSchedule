package com.example.myapplication.datamanager

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

class StoredVariables(private val context: Context) {
  companion object {
    private val Context.dataStore by preferencesDataStore("variables") // оно где-то хранится с именем variables
  }

  suspend fun setCurrentWeek(internalWeek: Int) = setInt("currentWeek", internalWeek)
  suspend fun getCurrentWeek() = getInt("currentWeek")
//  val flowCurrentWeek: Flow<Int?> = flowInt("currentWeek")

  suspend fun setSubgroup(subgroup: Int) = setInt("subgroup", subgroup)
  suspend fun getSubgroup() = getInt("subgroup")
  val fastSubgroup: Int get() {
    val subgroup: Int
    runBlocking {
      subgroup = getInt("subgroup") ?: 0
    }
    return subgroup
  }

  suspend fun setLink(uri: String) = setString("link", uri)
  suspend fun getLink() = getString("link")
  val fastLink: String get() {
    val link: String
    runBlocking {
      link = getString("link") ?: ""
    }
    return link
  }

  suspend fun setLastUpdate(internalWeek: String) = setString("lastUpdate", internalWeek)
  suspend fun getLastUpdate() = getString("lastUpdate")


  private suspend fun setInt(key: String, value: Int) {
    val dataStoreKey = intPreferencesKey(key)
    context.dataStore.edit { settings ->
      settings[dataStoreKey] = value
    }
  }
  private suspend fun getInt(key: String): Int? {
    val dataStoreKey = intPreferencesKey(key)
    val preferences = context.dataStore.data.first()
    return preferences[dataStoreKey]
  }
  private fun flowInt(key: String): Flow<Int?> {
    val dataStoreKey = intPreferencesKey(key)
    return context.dataStore.data.map { storage ->
      storage[dataStoreKey]
    }
  }

  private suspend fun setString(key: String, value: String) {
    val dataStoreKey = stringPreferencesKey(key)
    context.dataStore.edit { settings ->
      settings[dataStoreKey] = value
    }
  }
  // запись данных в основном потоке
//  binding.btnSave.setOnClickListener {
//    lifecycleScope.launch {
//      dm.setStr("key", "data")
//    }
//  }

  private suspend fun getString(key: String): String? {
    val dataStoreKey = stringPreferencesKey(key)
    val preferences = context.dataStore.data.first()
    return preferences[dataStoreKey]
  }
  // чтение данных в основном потоке
//  binding.btnRead.setOnClickListener {
//    lifecycleScope.launch {
//      val value = dm.getStr("key")
//      binding.tvReadValue.text = value ?: "No value found"
//    }
//  }

  private fun flowString(key: String): Flow<String?> {
    val dataStoreKey = stringPreferencesKey(key)
    return context.dataStore.data.map { storage ->
      storage[dataStoreKey]
    }
  }
  // автоматически изменяет что-либо при изменении значения "key", запускать из основного потока
//  lifecycleScope.launch {
//    dm.flowStr("key").collect { data ->
//      textInput.setText(data)
//    }
//  }



  /*
  booleanPreferencesKey(name: String)
  doublePreferencesKey(name: String)
  floatPreferencesKey(name: String)
  intPreferencesKey(name: String)
  longPreferencesKey(name: String)
  stringPreferencesKey(name: String)
  stringSetPreferencesKey(name: String)
   */
}
