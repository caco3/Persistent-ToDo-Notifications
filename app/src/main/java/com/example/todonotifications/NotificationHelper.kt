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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val raw = todoId.toLongOrNull()?.toInt() ?: todoId.hashCode()
        val id = if (raw == Int.MIN_VALUE) Int.MAX_VALUE else kotlin.math.abs(raw)
        return if (id == NOTIFICATION_ID_SUMMARY) id + 1 else id
    }

    fun buildDoneLabel(context: Context, todo: TodoItem): String {
        if (!todo.isRecurring) {
            return "${context.getString(R.string.done_action)} (${context.getString(R.string.done_suffix_delete)})"
        }
        val anchor = if (todo.dtStart > 0L) maxOf(todo.dtStart, System.currentTimeMillis())
                     else System.currentTimeMillis()
        val searchFrom = java.util.Calendar.getInstance().run {
            timeInMillis = anchor
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
            timeInMillis
        }
        val next = CalendarTodoSource.findNextInstanceAfter(context, todo.id, searchFrom)
        val suffix = if (next != null)
            SimpleDateFormat("d. MMMM yyyy", Locale.getDefault()).format(Date(next))
        else
            context.getString(R.string.done_suffix_recurring_unknown)
        return "${context.getString(R.string.done_action)} (${context.getString(R.string.done_suffix_recurs_at, suffix)})"
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
        val notifId = getNotificationIdForTodo(todo.id)
        val todoActionIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, TodoActionActivity::class.java).apply {
                putExtra(NotificationActionReceiver.EXTRA_TODO_ID, todo.id)
                putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
                putExtra(NotificationActionReceiver.EXTRA_IS_RECURRING, todo.isRecurring)
                putExtra(NotificationActionReceiver.EXTRA_DT_START, todo.dtStart)
                putExtra(NotificationActionReceiver.EXTRA_TITLE, todo.title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentTitle(todo.title)
            .setContentIntent(todoActionIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(GROUP_KEY)
        if (todo.calendarColor != null) builder.setColor(todo.calendarColor)
        if (todo.dtStart > 0L) {
            val date = Date(todo.dtStart)
            val label = "${SimpleDateFormat("d. MMMM yyyy", Locale.getDefault()).format(date)}" +
                    "  ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(date)}"
            builder.setContentText(label)
                .setWhen(todo.dtStart)
                .setShowWhen(true)
        }
        return builder.build()
    }
}
