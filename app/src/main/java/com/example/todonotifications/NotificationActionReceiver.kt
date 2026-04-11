package com.example.todonotifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPOST = "com.example.todonotifications.ACTION_REPOST"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPOST) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val todos = CalendarTodoSource.getTodos(context)
        todos.forEach { todo ->
            val id = NotificationHelper.getNotificationIdForTodo(todo.id)
            nm.cancel(id)
            nm.notify(id, NotificationHelper.buildTodoNotification(context, todo))
        }
        nm.notify(
            NotificationHelper.NOTIFICATION_ID_SUMMARY,
            NotificationHelper.buildSummaryNotification(context, todos)
        )
    }
}
