package com.example.todonotifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPOST = "com.example.todonotifications.ACTION_REPOST"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REPOST) {
            NotificationHelper.postTodoNotification(context)
        }
    }
}
