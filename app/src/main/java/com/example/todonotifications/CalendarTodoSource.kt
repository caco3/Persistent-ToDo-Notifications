package com.example.todonotifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import java.util.Calendar

object CalendarTodoSource {

    private const val TAG = "CalendarTodoSource"
    const val CALENDAR_NAME = "ToDo"

    fun getTodos(context: Context, ignoreFilters: Boolean = false): List<TodoItem> {
        if (!hasCalendarPermission(context)) return emptyList()

        val calendarIds = findCalendarIds(context)
        if (calendarIds.isEmpty()) {
            Log.w(TAG, "No calendar named '$CALENDAR_NAME' found")
            return emptyList()
        }

        val todos = mutableListOf<TodoItem>()
        val idPlaceholders = calendarIds.joinToString(" OR ") {
            "${CalendarContract.Events.CALENDAR_ID} = ?"
        }
        val selectionArgs = calendarIds.map { it.toString() }.toTypedArray()

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.STATUS
            ),
            "($idPlaceholders) AND ${CalendarContract.Events.DELETED} != 1",
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            val idCol     = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleCol  = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startCol  = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val statusCol = it.getColumnIndexOrThrow(CalendarContract.Events.STATUS)

            while (it.moveToNext()) {
                if (it.getInt(statusCol) == CalendarContract.Events.STATUS_CANCELED) continue
                val title = it.getString(titleCol)?.takeIf { t -> t.isNotBlank() } ?: continue
                todos.add(
                    TodoItem(
                        id      = it.getLong(idCol).toString(),
                        title   = title,
                        dtStart = it.getLong(startCol)
                    )
                )
            }
        }

        if (ignoreFilters) return todos

        val showOld = AppPreferences.getShowOldEvents(context)
        val jan2026 = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val filtered = if (showOld) todos else todos.filter { it.dtStart >= jan2026 }

        if (!AppPreferences.getNearOnly(context)) return filtered
        val oneWeekMs = 7L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return filtered.filter { it.dtStart in (now - oneWeekMs)..(now + oneWeekMs) }
    }

    fun findCalendarIds(context: Context): List<Long> {
        if (!hasCalendarPermission(context)) return emptyList()
        val ids = mutableListOf<Long>()
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            ),
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?",
            arrayOf(CALENDAR_NAME),
            null
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            while (it.moveToNext()) {
                ids.add(it.getLong(idCol))
            }
        }
        return ids
    }

    fun hasCalendarPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
}
