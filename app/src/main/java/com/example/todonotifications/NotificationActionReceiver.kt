package com.example.todonotifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.example.todonotifications.ACTION_COMPLETE"
        const val ACTION_REPOST  = "com.example.todonotifications.ACTION_REPOST"
        const val EXTRA_TODO_ID  = "extra_todo_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COMPLETE -> {
                val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
                TodoPreferences(context).completeTodo(todoId)
                NotificationHelper.postTodoNotification(context)
            }
            ACTION_REPOST -> {
                NotificationHelper.postTodoNotification(context)
            }
        }
    }
}
