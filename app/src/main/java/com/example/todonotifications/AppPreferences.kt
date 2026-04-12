package com.example.todonotifications

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SHOW_OLD_EVENTS = "show_old_events"
    private const val KEY_DAYS_BEFORE = "days_before"
    private const val KEY_DAYS_AFTER = "days_after"
    private const val KEY_DEMO_MODE = "demo_mode"
    private const val KEY_CALENDAR_NAME = "calendar_name"
    private const val KEY_CALENDAR_NAMES = "calendar_names"
    const val DEFAULT_CALENDAR_NAME = "ToDo"

    fun getShowOldEvents(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_OLD_EVENTS, false)

    fun setShowOldEvents(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_OLD_EVENTS, value).apply()
    }

    fun getDaysBefore(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DAYS_BEFORE, 30)

    fun setDaysBefore(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DAYS_BEFORE, value).apply()
    }

    fun getDaysAfter(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DAYS_AFTER, 30)

    fun setDaysAfter(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DAYS_AFTER, value).apply()
    }

    fun getDemoMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEMO_MODE, false)

    fun setDemoMode(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DEMO_MODE, value).apply()
    }

    fun getCalendarName(context: Context): String =
        getCalendarNames(context).firstOrNull() ?: DEFAULT_CALENDAR_NAME

    fun getCalendarNames(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_CALENDAR_NAMES)) {
            val legacy = prefs.getString(KEY_CALENDAR_NAME, DEFAULT_CALENDAR_NAME) ?: DEFAULT_CALENDAR_NAME
            val migrated = setOf(legacy)
            prefs.edit().putStringSet(KEY_CALENDAR_NAMES, migrated).apply()
            return migrated
        }
        return prefs.getStringSet(KEY_CALENDAR_NAMES, setOf(DEFAULT_CALENDAR_NAME)) ?: setOf(DEFAULT_CALENDAR_NAME)
    }

    fun setCalendarNames(context: Context, names: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_CALENDAR_NAMES, names).apply()
    }

    fun setCalendarName(context: Context, value: String) {
        setCalendarNames(context, setOf(value.trim()))
    }

    private const val KEY_SNOOZED_TODOS = "snoozed_todos"

    fun snoozeTodo(context: Context, todoId: String, durationMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_SNOOZED_TODOS, emptySet())!!.toMutableSet()
        existing.removeAll { it.startsWith("$todoId:") }
        val expiry = System.currentTimeMillis() + durationMs
        existing.add("$todoId:$expiry")
        prefs.edit().putStringSet(KEY_SNOOZED_TODOS, existing).apply()
    }

    fun clearSnooze(context: Context, todoId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_SNOOZED_TODOS, emptySet())!!.toMutableSet()
        existing.removeAll { it.startsWith("$todoId:") }
        prefs.edit().putStringSet(KEY_SNOOZED_TODOS, existing).apply()
    }

    fun isSnoozed(context: Context, todoId: String): Boolean {
        val now = System.currentTimeMillis()
        val snoozed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SNOOZED_TODOS, emptySet()) ?: return false
        return snoozed.any { entry ->
            val parts = entry.split(":", limit = 2)
            parts.size == 2 && parts[0] == todoId && (parts[1].toLongOrNull() ?: 0L) > now
        }
    }

    fun clearExpiredSnoozes(context: Context) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_SNOOZED_TODOS, emptySet())!!.toMutableSet()
        val cleaned = existing.filterTo(mutableSetOf()) { entry ->
            val parts = entry.split(":", limit = 2)
            parts.size == 2 && (parts[1].toLongOrNull() ?: 0L) > now
        }
        if (cleaned.size != existing.size) {
            prefs.edit().putStringSet(KEY_SNOOZED_TODOS, cleaned).apply()
        }
    }

    private const val KEY_HANDLED_UNTIL = "handled_until"

    fun setHandledUntil(context: Context, todoId: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_HANDLED_UNTIL, emptySet())!!.toMutableSet()
        existing.removeAll { it.startsWith("$todoId:") }
        existing.add("$todoId:${endOfDay(timestamp)}")
        prefs.edit().putStringSet(KEY_HANDLED_UNTIL, existing).apply()
    }

    private fun endOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun getHandledUntil(context: Context, todoId: String): Long {
        val set = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_HANDLED_UNTIL, emptySet()) ?: return 0L
        return set.firstOrNull { it.startsWith("$todoId:") }
            ?.split(":", limit = 2)?.getOrNull(1)?.toLongOrNull() ?: 0L
    }

    fun getNextSnoozeExpiry(context: Context): Long? {
        val now = System.currentTimeMillis()
        val snoozed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SNOOZED_TODOS, emptySet()) ?: return null
        return snoozed.mapNotNull { entry ->
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2) parts[1].toLongOrNull() else null
        }.filter { it > now }.minOrNull()
    }
}
