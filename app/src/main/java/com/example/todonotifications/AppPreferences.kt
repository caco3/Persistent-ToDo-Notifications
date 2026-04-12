package com.example.todonotifications

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SHOW_OLD_EVENTS = "show_old_events"
    private const val KEY_DAYS_BEFORE = "days_before"
    private const val KEY_DAYS_AFTER = "days_after"
    private const val KEY_DEMO_MODE = "demo_mode"
    private const val KEY_CALENDAR_NAME = "calendar_name"
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
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CALENDAR_NAME, DEFAULT_CALENDAR_NAME) ?: DEFAULT_CALENDAR_NAME

    fun setCalendarName(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CALENDAR_NAME, value.trim()).apply()
    }
}
