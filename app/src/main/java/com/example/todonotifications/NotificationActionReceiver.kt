package com.example.todonotifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPOST = "com.example.todonotifications.ACTION_REPOST"
        const val ACTION_DELETE_TODO = "com.example.todonotifications.ACTION_DELETE_TODO"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_NOTIF_ID = "notif_id"
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
        }
    }
}
