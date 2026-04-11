package com.example.todonotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import java.text.DateFormat
import java.util.Date

object NotificationHelper {

    const val CHANNEL_ID = "todo_persistent_channel"
    const val NOTIFICATION_ID_SUMMARY = 1001
    const val GROUP_KEY = "com.example.todonotifications.TODOS"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun postTodoNotification(context: Context) {
        TodoForegroundService.startOrUpdate(context)
    }

    fun getNotificationIdForTodo(todoId: String): Int {
        val raw = todoId.toLongOrNull()?.rem(900_000L)?.toInt()
            ?: (todoId.hashCode() and 0x7FFF_FFFF) % 900_000
        return (if (raw < 0) raw + 900_000 else raw) + 100_000
    }

    fun buildOpenEventIntent(context: Context, eventId: String): Intent {
        val uri = "${CalendarContract.Events.CONTENT_URI}/$eventId".toUri()
        val bc2Intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.appgenix.bizcal")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (context.packageManager.resolveActivity(
                bc2Intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            bc2Intent
        } else {
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun buildSummaryNotification(context: Context, todos: List<TodoItem>): Notification {
        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val repostIntent = PendingIntent.getBroadcast(
            context, 997,
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
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setSubText("build: ${BuildConfig.GIT_HASH}")

        if (todos.isEmpty()) {
            builder
                .setContentTitle(context.getString(R.string.notification_title_done))
                .setContentText(context.getString(R.string.notification_text_done))
        } else {
            val title = context.resources.getQuantityString(
                R.plurals.notification_title_count, todos.size, todos.size)
            val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
            todos.take(6).forEach { style.addLine("• ${it.title}") }
            if (todos.size > 6) {
                style.setSummaryText(
                    context.getString(R.string.notification_more, todos.size - 6))
            }
            builder
                .setContentTitle(title)
                .setContentText(todos.first().title)
                .setStyle(style)
        }
        return builder.build()
    }

    fun buildTodoNotification(context: Context, todo: TodoItem): Notification {
        val openEventIntent = PendingIntent.getActivity(
            context,
            getNotificationIdForTodo(todo.id),
            buildOpenEventIntent(context, todo.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentTitle(todo.title)
            .setContentIntent(openEventIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(GROUP_KEY)
        if (todo.dtStart > 0L) {
            val date = Date(todo.dtStart)
            val label = "${DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)}" +
                    "  ${DateFormat.getTimeInstance(DateFormat.SHORT).format(date)}"
            builder.setContentText(label)
                .setWhen(todo.dtStart)
                .setShowWhen(true)
        }
        return builder.build()
    }
}
