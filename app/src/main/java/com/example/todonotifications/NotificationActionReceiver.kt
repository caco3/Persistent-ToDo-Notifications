package com.example.todonotifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPOST = "com.example.todonotifications.ACTION_REPOST"
        const val ACTION_DELETE_TODO = "com.example.todonotifications.ACTION_DELETE_TODO"
        const val ACTION_SNOOZE_TODO = "com.example.todonotifications.ACTION_SNOOZE_TODO"
        const val ACTION_DONE_RECURRING = "com.example.todonotifications.ACTION_DONE_RECURRING"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_SNOOZE_DURATION_MS = "snooze_duration_ms"
        const val EXTRA_DT_START = "dt_start"
        const val EXTRA_IS_RECURRING = "is_recurring"
        const val EXTRA_TITLE = "title"
        private const val SNOOZE_ALARM_REQUEST_CODE = 998
        private const val DONE_ALARM_REQUEST_CODE = 999

        fun scheduleNextOccurrenceAlarm(context: Context, todoId: String, dtStart: Long) {
            val now = System.currentTimeMillis()
            val afterMs = AppPreferences.getDaysAfter(context) * 24L * 60 * 60 * 1000
            val next = CalendarTodoSource.findNextInstanceAfter(context, todoId, dtStart)
            val fireAt = if (next != null) maxOf(next - afterMs, now + 1_000L) else now + 1_000L
            val am = context.getSystemService(AlarmManager::class.java)
            val pi = PendingIntent.getBroadcast(
                context, DONE_ALARM_REQUEST_CODE,
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = ACTION_REPOST
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }

        fun scheduleSnoozeWakeup(context: Context, delayMs: Long) {
            val am = context.getSystemService(AlarmManager::class.java)
            val pi = PendingIntent.getBroadcast(
                context, SNOOZE_ALARM_REQUEST_CODE,
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = ACTION_REPOST
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMs,
                pi
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REPOST -> TodoForegroundService.startOrUpdate(context)
            ACTION_DELETE_TODO -> {
                val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
                val uri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI, todoId.toLong()
                )
                context.contentResolver.delete(uri, null, null)
                if (notifId != -1) {
                    context.getSystemService(NotificationManager::class.java).cancel(notifId)
                }
                TodoForegroundService.startOrUpdate(context)
            }
            ACTION_SNOOZE_TODO -> {
                val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
                val durationMs = intent.getLongExtra(EXTRA_SNOOZE_DURATION_MS, 60 * 60 * 1000L)
                AppPreferences.snoozeTodo(context, todoId, durationMs)
                AppLogger.log(context, "SNOOZE  id=$todoId  durationMs=$durationMs")
                if (notifId != -1) {
                    context.getSystemService(NotificationManager::class.java).cancel(notifId)
                }
                scheduleSnoozeWakeup(context, durationMs)
                TodoForegroundService.startOrUpdate(context)
            }
            ACTION_DONE_RECURRING -> {
                val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
                val dtStart = intent.getLongExtra(EXTRA_DT_START, 0L)
                AppPreferences.setHandledUntil(context, todoId, dtStart)
                AppLogger.log(context, "DONE (recurring)  id=$todoId  dtStart=$dtStart")
                if (notifId != -1) {
                    context.getSystemService(NotificationManager::class.java).cancel(notifId)
                }
                scheduleNextOccurrenceAlarm(context, todoId, dtStart)
                TodoForegroundService.startOrUpdate(context)
            }
        }
    }

}
