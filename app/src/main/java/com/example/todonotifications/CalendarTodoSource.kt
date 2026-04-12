package com.example.todonotifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import java.util.Calendar

object CalendarTodoSource {

    private const val TAG = "CalendarTodoSource"

    fun getTodos(context: Context, ignoreFilters: Boolean = false): List<TodoItem> {
        if (!hasCalendarPermission(context)) return emptyList()

        val calendarIds = findCalendarIds(context)
        if (calendarIds.isEmpty()) {
            Log.w(TAG, "No calendar named '${AppPreferences.getCalendarName(context)}' found")
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
                CalendarContract.Events.STATUS,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.ORIGINAL_ID
            ),
            "($idPlaceholders) AND ${CalendarContract.Events.DELETED} != 1",
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            val idCol         = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleCol      = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startCol      = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val statusCol     = it.getColumnIndexOrThrow(CalendarContract.Events.STATUS)
            val rruleCol      = it.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            val originalIdCol = it.getColumnIndexOrThrow(CalendarContract.Events.ORIGINAL_ID)

            while (it.moveToNext()) {
                if (it.getInt(statusCol) == CalendarContract.Events.STATUS_CANCELED) continue
                val title = it.getString(titleCol)?.takeIf { t -> t.isNotBlank() } ?: continue
                val isRecurring = !it.getString(rruleCol).isNullOrEmpty() ||
                        !it.getString(originalIdCol).isNullOrEmpty()
                todos.add(
                    TodoItem(
                        id          = it.getLong(idCol).toString(),
                        title       = title,
                        dtStart     = it.getLong(startCol),
                        isRecurring = isRecurring
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

        val now = System.currentTimeMillis()
        if (AppPreferences.getNearOnly(context)) {
            val oneWeekMs = 7L * 24 * 60 * 60 * 1000
            return filtered.filter { it.dtStart in (now - oneWeekMs)..(now + oneWeekMs) }
        }
        if (AppPreferences.getMonthOnly(context)) {
            val oneMonthMs = 30L * 24 * 60 * 60 * 1000
            return filtered.filter { it.dtStart in (now - oneMonthMs)..(now + oneMonthMs) }
        }
        return filtered
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
            arrayOf(AppPreferences.getCalendarName(context)),
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

    fun getDummyTodos(): List<TodoItem> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        return listOf(
            TodoItem("1", "Call dentist to reschedule appointment",   now - 2 * day),
            TodoItem("2", "Review Q2 budget report",                   now - day),
            TodoItem("3", "Buy birthday gift for Anna",                now + day),
            TodoItem("4", "Submit expense report",                     now + 2 * day),
            TodoItem("5", "Team standup preparation",                  now + 3 * day),
            TodoItem("6", "Renew car insurance",                       now + 5 * day),
            TodoItem("7", "Book hotel for conference",                 now + 8 * day),
            TodoItem("8", "Finish slide deck for product review",      now + 10 * day),
            TodoItem("9", "Pay quarterly taxes",                       now + 14 * day),
            TodoItem("10", "Schedule annual checkup",                  now + 18 * day),
            TodoItem("11", "Order replacement laptop charger",         now + 21 * day),
            TodoItem("12", "Plan team offsite agenda",                 now + 25 * day)
        )
    }

    fun hasCalendarPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
}
