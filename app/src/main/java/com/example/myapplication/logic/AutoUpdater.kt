package com.example.myapplication.logic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.datamanager.DataManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AutoUpdater : BroadcastReceiver() {
  companion object {
    fun updateAfterSomeMinutes(context: Context, minutes: Long) {
      val intent = Intent(context, AutoUpdater::class.java)

      val alarmUp = PendingIntent.getBroadcast(
        context, 0, intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        else
          PendingIntent.FLAG_NO_CREATE
      ) != null

      if (alarmUp) {
        Log.d("mxkmnAlarm", "Alarm is already active")
      } else {
        Log.d("mxkmnAlarm", "Alarm is not active")

        val pendingIntent = PendingIntent.getBroadcast (
          context, 0, intent,
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          else
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        mAlarmManager.setRepeating( AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), minutes*60000, pendingIntent )

        Log.i("mxkmnAutoUpdater", "New update + waiting $minutes minutes by onReceive requested...")
        Toast.makeText(context, "New update + waiting $minutes minutes...", Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
//    Log.i("mxkmnAutoUpdater", "onReceive called: let's update!")
//    Toast.makeText(context, "onReceive called", Toast.LENGTH_SHORT).show()

    val storage = DataManager(context) // получение обновы
    storage.updateAndDeleteOutdatedWeek()
  }
}