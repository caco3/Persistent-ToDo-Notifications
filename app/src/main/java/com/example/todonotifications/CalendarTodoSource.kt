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
        val calendarColorMap = buildCalendarColorMap(context, calendarIds)

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
                CalendarContract.Events.ORIGINAL_ID,
                CalendarContract.Events.CALENDAR_ID
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
            val calIdCol      = it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)

            while (it.moveToNext()) {
                if (it.getInt(statusCol) == CalendarContract.Events.STATUS_CANCELED) continue
                val title = it.getString(titleCol)?.takeIf { t -> t.isNotBlank() } ?: continue
                val isRecurring = !it.getString(rruleCol).isNullOrEmpty() ||
                        !it.getString(originalIdCol).isNullOrEmpty()
                todos.add(
                    TodoItem(
                        id            = it.getLong(idCol).toString(),
                        title         = title,
                        dtStart       = it.getLong(startCol),
                        isRecurring   = isRecurring,
                        calendarColor = calendarColorMap[it.getLong(calIdCol)]
                    )
                )
            }
        }

        val recurringIds = todos.filter { it.isRecurring }.map { it.id }.toSet()
        if (recurringIds.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000L
            val beforeMs = AppPreferences.getDaysBefore(context) * dayMs
            val afterMs  = AppPreferences.getDaysAfter(context)  * dayMs
            val windowStart = now - beforeMs
            val windowEnd   = now + afterMs
            val instanceUri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(windowStart.toString())
                .appendPath(windowEnd.toString())
                .build()
            val allInstances = mutableMapOf<String, MutableList<Long>>()
            context.contentResolver.query(
                instanceUri,
                arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.BEGIN),
                null, null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { c ->
                val eidCol   = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val beginCol = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                while (c.moveToNext()) {
                    val eid = c.getLong(eidCol).toString()
                    if (eid in recurringIds)
                        allInstances.getOrPut(eid) { mutableListOf() }.add(c.getLong(beginCol))
                }
            }
            val instanceMap = mutableMapOf<String, Long>()
            for ((eid, instances) in allInstances) {
                val handledUntil = AppPreferences.getHandledUntil(context, eid)
                val unhandled = instances.filter { it > handledUntil }
                val chosen = unhandled.lastOrNull { it <= now } ?: unhandled.firstOrNull()
                Log.d(TAG, "eid=$eid instances=${instances.size} handledUntil=$handledUntil unhandled=${unhandled.size} chosen=$chosen")
                if (chosen != null) instanceMap[eid] = chosen
            }
            val updated = todos.mapNotNull { todo ->
                val instance = instanceMap[todo.id]
                when {
                    !todo.isRecurring -> todo
                    instance != null -> todo.copy(dtStart = instance)
                    else -> {
                        val handledUntil = AppPreferences.getHandledUntil(context, todo.id)
                        Log.d(TAG, "recurring id=${todo.id} title='${todo.title}' dtStart=${todo.dtStart} handledUntil=$handledUntil inWindow=${todo.id in allInstances}")
                        if (handledUntil >= todo.dtStart) null else todo
                    }
                }
            }
            todos.clear()
            todos.addAll(updated.sortedBy { it.dtStart })
        }

        if (ignoreFilters) return todos

        val showOld = AppPreferences.getShowOldEvents(context)
        val jan2026 = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val filtered = if (showOld) todos else todos.filter { it.isRecurring || it.dtStart >= jan2026 }

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val beforeMs = AppPreferences.getDaysBefore(context) * dayMs
        val afterMs  = AppPreferences.getDaysAfter(context)  * dayMs
        val rangeFiltered = filtered.filter { it.dtStart in (now - beforeMs)..(now + afterMs) }

        AppPreferences.clearExpiredSnoozes(context)
        val result = rangeFiltered.filter { !AppPreferences.isSnoozed(context, it.id) }
        return result.map { todo ->
            if (!todo.isRecurring) todo else {
                val anchor = Calendar.getInstance().run {
                    timeInMillis = maxOf(todo.dtStart, now)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                    timeInMillis
                }
                todo.copy(nextDtStart = findNextInstanceAfter(context, todo.id, anchor))
            }
        }
    }

    fun getAvailableCalendarNames(context: Context): List<String> {
        if (!hasCalendarPermission(context)) return emptyList()
        val names = mutableListOf<String>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
            null, null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )?.use { c ->
            val col = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            while (c.moveToNext()) {
                c.getString(col)?.takeIf { it.isNotBlank() }?.let { names.add(it) }
            }
        }
        return names.distinct()
    }

    fun getCalendarNameColorMap(context: Context): Map<String, Int> {
        if (!hasCalendarPermission(context)) return emptyMap()
        val names = AppPreferences.getCalendarNames(context)
        if (names.isEmpty()) return emptyMap()
        val placeholders = names.joinToString(",") { "?" }
        val map = mutableMapOf<String, Int>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR
            ),
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} IN ($placeholders)",
            names.toTypedArray(),
            null
        )?.use { c ->
            val nameCol  = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val colorCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            while (c.moveToNext()) {
                val name = c.getString(nameCol) ?: continue
                map[name] = c.getInt(colorCol)
            }
        }
        return map
    }

    private fun buildCalendarColorMap(context: Context, calendarIds: List<Long>): Map<Long, Int> {
        val map = mutableMapOf<Long, Int>()
        val placeholders = calendarIds.joinToString(",") { "?" }
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_COLOR),
            "${CalendarContract.Calendars._ID} IN ($placeholders)",
            calendarIds.map { it.toString() }.toTypedArray(),
            null
        )?.use { c ->
            val idCol    = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val colorCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            while (c.moveToNext()) map[c.getLong(idCol)] = c.getInt(colorCol)
        }
        return map
    }

    fun findCalendarIds(context: Context): List<Long> {
        if (!hasCalendarPermission(context)) return emptyList()
        val names = AppPreferences.getCalendarNames(context)
        if (names.isEmpty()) return emptyList()
        val placeholders = names.joinToString(",") { "?" }
        val ids = mutableListOf<Long>()
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            ),
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} IN ($placeholders)",
            names.toTypedArray(),
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

    fun findNextInstanceAfter(context: Context, todoId: String, afterTimestamp: Long): Long? {
        if (!hasCalendarPermission(context)) return null
        val end = afterTimestamp + 2 * 365L * 24 * 60 * 60 * 1000
        val instanceUri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath((afterTimestamp + 1).toString())
            .appendPath(end.toString())
            .build()
        context.contentResolver.query(
            instanceUri,
            arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.BEGIN),
            "${CalendarContract.Instances.EVENT_ID} = ?",
            arrayOf(todoId),
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { c ->
            val beginCol = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            if (c.moveToFirst()) return c.getLong(beginCol)
        }
        return null
    }

    fun getDummyTodos(): List<TodoItem> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        return listOf(
            TodoItem("1", "Call dentist to reschedule appointment",   now - 2 * day),
            TodoItem("2", "Review Q2 budget report",                   now - day),
            TodoItem("3", "Weekly team sync",                          now + day,      isRecurring = true, nextDtStart = now + 8 * day),
            TodoItem("4", "Buy birthday gift for Anna",                now + 2 * day),
            TodoItem("5", "Submit expense report",                     now + 3 * day),
            TodoItem("6", "Monthly 1:1 with manager",                  now + 5 * day,  isRecurring = true, nextDtStart = now + 35 * day),
            TodoItem("7", "Renew car insurance",                       now + 8 * day),
            TodoItem("8", "Book hotel for conference",                 now + 10 * day),
            TodoItem("9", "Quarterly taxes",                           now + 14 * day, isRecurring = true, nextDtStart = now + 104 * day),
            TodoItem("10", "Finish slide deck for product review",     now + 18 * day),
            TodoItem("11", "Annual checkup",                           now + 21 * day, isRecurring = true, nextDtStart = now + 386 * day),
            TodoItem("12", "Plan team offsite agenda",                 now + 25 * day)
        )
    }

    fun hasCalendarPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
}
