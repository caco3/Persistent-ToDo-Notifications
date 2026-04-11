package com.example.todonotifications

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SHOW_OLD_EVENTS = "show_old_events"

    fun getShowOldEvents(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_OLD_EVENTS, false)

    fun setShowOldEvents(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_OLD_EVENTS, value).apply()
    }
}
