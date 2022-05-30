package com.example.myapplication.logic

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.ViewTreeLifecycleOwner.set
import com.example.myapplication.datamanager.Lesson
import com.example.myapplication.datamanager.StoredDatabase
import com.example.myapplication.view.MainActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import kotlin.concurrent.thread


class CalendarWorker(private val contentResolver: ContentResolver, private val activity: MainActivity, private val defaultLink: String) {
  private val _accountName = "Расписание"
  private val _placeName = "Иркутский политех"

  private val _accessibleNowEmitter = MutableSharedFlow<Unit>()
  val flowAccessibleNow = _accessibleNowEmitter.asSharedFlow()
  var isAccessible = true

  private fun getSubgroupStr(subgroup: Int): String = if (subgroup == 0) "" else " (подгруппа $subgroup)"
  private fun getLinkStr(link: String, type: String): String = if (link.isBlank()) "" else "\nСсылка на $type: $defaultLink$link"

  private fun concatStrings(oldString: String, newString: String, separatorType: Int = 0): String {
    return when {
      newString.isEmpty() -> oldString
      oldString.isEmpty() -> newString.replaceFirstChar { it.uppercase() }
      else -> {
        val separator = when (separatorType) {
          1 -> " | "
          2 -> ", "
          else -> ""
        }
        "$oldString$separator$newString"
      }
    }
  }

  fun addSchedule(lessons: List<Lesson>) {
    isAccessible = false
    Log.i("mxkmnCalendar", "started")
    if (!checkPermissions())
      return

    if (getCalendarsId().isEmpty())
      createCalendar()
    else { // delete all events from calendar
      val selection = "${Events.CALENDAR_ID} = ?"
      val selectionArgs = arrayOf(getCalendarsId()[0].toString())
      contentResolver.delete(Events.CONTENT_URI, selection, selectionArgs)
    }

    for (lesson in lessons) {
      val title = concatStrings(concatStrings(lesson.classroomName, lesson.name, 1), getSubgroupStr(lesson.subgroup)) // В304 | Информатика (подгруппа 2)
      var description = concatStrings(lesson.type, lesson.teacherName, 2) // Лекция, Петров И.И.
      description = concatStrings(description, getLinkStr(lesson.teacherLink, "преподавателя")) // Ссылка на преподавателя: www
      description = concatStrings(description, getLinkStr(lesson.classroomLink, "аудиторию")) // Ссылка на аудиторию: www
      addEvent(title, description, Time.getLessonStartDateTime(lesson), 90)
    }
    isAccessible = true
    Log.i("mxkmnCalendar", "ended")
    runBlocking {
      _accessibleNowEmitter.emit(Unit)
    }
  }

  private fun checkPermissions(): Boolean {
    val requestedPermissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    var permissions = true
    for (p in requestedPermissions) {
      permissions = permissions && ContextCompat.checkSelfPermission(activity, p) == PermissionChecker.PERMISSION_GRANTED
    }
    if (!permissions) ActivityCompat.requestPermissions(activity, requestedPermissions, 123)
    return permissions
  }

  private fun getCalendarsId(): List<Long> {
    val projection = arrayOf(CalendarContract.Calendars._ID)
    val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"

    val selArgs = arrayOf(_accountName, CalendarContract.ACCOUNT_TYPE_LOCAL ) // use the same values as above
    val cursor = contentResolver.query( CalendarContract.Calendars.CONTENT_URI, projection, selection, selArgs, null )!!
    val list = mutableListOf<Long>()
    if (cursor.moveToFirst()) {
      do {
        list.add(cursor.getLong(0))
      } while (cursor.moveToNext())
    }
    cursor.close()
    return list
  }

  private fun createCalendar() {
    val values = ContentValues()
    values.put( CalendarContract.Calendars.ACCOUNT_NAME, _accountName)
    values.put( CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL )
    values.put( CalendarContract.Calendars.NAME, _placeName )
    values.put( CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, _placeName )
    values.put( CalendarContract.Calendars.CALENDAR_COLOR, -0x10000 )
    values.put( CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER )
    values.put( CalendarContract.Calendars.OWNER_ACCOUNT, "some.account@googlemail.com" )
    values.put( CalendarContract.Calendars.CALENDAR_TIME_ZONE, "Asia/Irkutsk" )
    values.put( CalendarContract.Calendars.SYNC_EVENTS, 1 )
    val builder: Uri.Builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
    builder.appendQueryParameter( CalendarContract.Calendars.ACCOUNT_NAME, "mxkmn" )
    builder.appendQueryParameter( CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL )
    builder.appendQueryParameter( CalendarContract.CALLER_IS_SYNCADAPTER, "true" )
    /*val uri: Uri? = */ contentResolver.insert(builder.build(), values)
  }

  private fun addEvent(title: String, description: String, startDate: LocalDateTime, durationInMinutes: Int) {
//    val endMillis: Long = Calendar.getInstance().run {
//      set(2012, 9, 14, 8, 45)
//      timeInMillis
//    }
    val millisecondsStart = startDate.atZone(ZoneId.of("Asia/Irkutsk")).toInstant().toEpochMilli()
    val millisecondsEnd = millisecondsStart + durationInMinutes * 60 * 1000

    val values = ContentValues()
    values.put(Events.DTSTART, millisecondsStart)
    values.put(Events.DTEND, millisecondsEnd)
    values.put(Events.TITLE, title)
    values.put(Events.DESCRIPTION, description)
    values.put(Events.CALENDAR_ID, getCalendarsId()[0])
    values.put(Events.EVENT_TIMEZONE, "Asia/Irkutsk")
    val uri: Uri? = contentResolver.insert(Events.CONTENT_URI, values)
//    Log.i("mxkmnCalendar", uri.toString())


//    val calId: Long = getCalendarsId()[0]
//    val cal = GregorianCalendar(2012, 11, 14)
//    cal.setTimeZone(TimeZone.getTimeZone("UTC"))
//    cal.set(Calendar.HOUR, 0)
//    cal.set(Calendar.MINUTE, 0)
//    cal.set(Calendar.SECOND, 0)
//    cal.set(Calendar.MILLISECOND, 0)
//    val start: Long = cal.getTimeInMillis()
//    val values = ContentValues()
//    values.put(Events.DTSTART, start)
//    values.put(Events.DTEND, start)
//    values.put(Events.RRULE, "FREQ=DAILY;COUNT=20;BYDAY=MO,TU,WE,TH,FR;WKST=MO")
//    values.put(Events.TITLE, "Some title")
//    values.put(Events.CALENDAR_ID, calId)
//    values.put(Events.EVENT_TIMEZONE, "Asia/Irkutsk")
//    values.put(Events.DESCRIPTION, "The agenda or some description of the event")
//    // reasonable defaults exist:
//    values.put(Events.ACCESS_LEVEL, Events.ACCESS_PRIVATE)
//    values.put( Events.SELF_ATTENDEE_STATUS, Events.STATUS_CONFIRMED )
//    values.put(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
//    val uri: Uri? = contentResolver.insert(Events.CONTENT_URI, values)
//    val eventId: Long = uri.lastPathSegment
//    Log.i("mxkmnCalendar", eventId.toString())
  }















//  private fun google() {
  // Run query
//    val uri: Uri = CalendarContract.Calendars.CONTENT_URI
//    val selection: String = "((${CalendarContract.Calendars.ACCOUNT_NAME} = ?) AND (${CalendarContract.Calendars.ACCOUNT_TYPE} = ?))"
//    val selectionArgs: Array<String> = arrayOf("mxkmnz@gmail.com", "com.example")
//    val cur = contentResolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, null)!!
//
//    // Use the cursor to step through the returned records
//    while (cur.moveToNext()) {
//      // Get the field values
//      val calID: Long = cur.getLong(PROJECTION_ID_INDEX)
//      val displayName: String = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
//      val accountName: String = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
//      val ownerName: String = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)
//      // Do something with the values...
//      Log.d("mxkmnCalendar", "Calendar ID: $calID, Display Name: $displayName, Account Name: $accountName, Owner Name: $ownerName")
//    }
//
//    cur.close()



//    val projection = arrayOf(
//      Calendars._ID,
//      Calendars.NAME,
//      Calendars.ACCOUNT_NAME,
//      Calendars.ACCOUNT_TYPE
//    )
//    val calCursor: Cursor = contentResolver.query(
//      Calendars.CONTENT_URI,
//      projection,
//      Calendars.VISIBLE + " = 1",
//      null,
//      Calendars._ID + " ASC"
//    )!!
//    if (calCursor.moveToFirst()) {
//      do {
//        val id: Long = calCursor.getLong(0)
//        val displayName: String = calCursor.getString(1)
//        Log.i("mxkmnCal", "Calendar ID: $id, Display Name: $displayName")
//      } while (calCursor.moveToNext())
//    }
//  }


  //  companion object {
//    // Projection array. Creating indices for this array instead of doing
//    // dynamic lookups improves performance.
//    private val EVENT_PROJECTION: Array<String> = arrayOf(
//      CalendarContract.Calendars._ID,                     // 0
//      CalendarContract.Calendars.ACCOUNT_NAME,            // 1
//      CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
//      CalendarContract.Calendars.OWNER_ACCOUNT            // 3
//    )
//
//    // The indices for the projection array above.
//    private const val PROJECTION_ID_INDEX: Int = 0
//    private const val PROJECTION_ACCOUNT_NAME_INDEX: Int = 1
//    private const val PROJECTION_DISPLAY_NAME_INDEX: Int = 2
//    private const val PROJECTION_OWNER_ACCOUNT_INDEX: Int = 3
//
//
//    private const val MY_ACCOUNT_NAME = "Расписание ИРНИТУ"
//  }
}