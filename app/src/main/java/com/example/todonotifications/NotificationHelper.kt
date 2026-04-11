package com.example.todonotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_ID = "todo_persistent_channel"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun postTodoNotification(context: Context) {
        TodoForegroundService.startOrUpdate(context)
    }

    fun buildNotification(context: Context): Notification {
        val todos = CalendarTodoSource.getTodos(context)

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val repostIntent = PendingIntent.getBroadcast(
            context,
            997,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_REPOST
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent)
            .setDeleteIntent(repostIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSubText("build: ${BuildConfig.GIT_HASH}")

        if (todos.isEmpty()) {
            builder
                .setContentTitle(context.getString(R.string.notification_title_done))
                .setContentText(context.getString(R.string.notification_text_done))
        } else {
            builder
                .setContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.notification_title_count, todos.size, todos.size
                    )
                )
                .setContentText(todos.first().title)

            val style = NotificationCompat.InboxStyle()
                .setBigContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.notification_title_count, todos.size, todos.size
                    )
                )

            todos.take(6).forEach { todo ->
                style.addLine("• ${todo.title}")
            }

            if (todos.size > 6) {
                style.setSummaryText(
                    context.getString(R.string.notification_more, todos.size - 6)
                )
            }

            builder.setStyle(style)
        }

        return builder.build()
    }
}
