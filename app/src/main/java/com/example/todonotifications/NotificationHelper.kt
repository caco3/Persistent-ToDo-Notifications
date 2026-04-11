package com.example.todonotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

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
        val todos = TodoPreferences(context).getTodos()
        val pending = todos.filter { !it.isCompleted }

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val addAction = buildAddAction(context)

        if (pending.isEmpty()) {
            builder
                .setContentTitle(context.getString(R.string.notification_title_done))
                .setContentText(context.getString(R.string.notification_text_done))
                .addAction(addAction)
        } else {
            builder
                .setContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.notification_title_count, pending.size, pending.size
                    )
                )
                .setContentText(pending.first().title)
                .addAction(buildCompleteAction(context, pending.first()))
                .addAction(addAction)

            val style = NotificationCompat.InboxStyle()
                .setBigContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.notification_title_count, pending.size, pending.size
                    )
                )

            pending.take(6).forEach { todo ->
                style.addLine("• ${todo.title}")
            }

            if (pending.size > 6) {
                style.setSummaryText(
                    context.getString(R.string.notification_more, pending.size - 6)
                )
            }

            builder.setStyle(style)
        }

        return builder.build()
    }

    private fun buildCompleteAction(context: Context, todo: TodoItem): NotificationCompat.Action {
        val intent = PendingIntent.getBroadcast(
            context,
            todo.id.hashCode(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COMPLETE
                putExtra(NotificationActionReceiver.EXTRA_TODO_ID, todo.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action(
            R.drawable.ic_check,
            context.getString(R.string.action_complete, todo.title.take(20)),
            intent
        )
    }

    private fun buildAddAction(context: Context): NotificationCompat.Action {
        val intent = PendingIntent.getActivity(
            context,
            999,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = MainActivity.ACTION_ADD_TODO
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action(
            R.drawable.ic_add,
            context.getString(R.string.action_add_todo),
            intent
        )
    }
}
