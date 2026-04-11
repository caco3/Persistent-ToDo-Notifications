package com.example.todonotifications

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SHOW_OLD_EVENTS = "show_old_events"
    private const val KEY_NEAR_ONLY = "near_only"
    private const val KEY_MONTH_ONLY = "month_only"

    fun getShowOldEvents(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_OLD_EVENTS, false)

    fun setShowOldEvents(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_OLD_EVENTS, value).apply()
    }

    fun getNearOnly(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NEAR_ONLY, false)

    fun setNearOnly(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NEAR_ONLY, value).apply()
    }

    fun getMonthOnly(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MONTH_ONLY, false)

    fun setMonthOnly(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MONTH_ONLY, value).apply()
    }
}
