package com.example.todonotifications

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

class TodoForegroundService : Service() {

    private var calendarObserver: ContentObserver? = null
    private val activeNotifIds = mutableSetOf<Int>()

    companion object {
        fun startOrUpdate(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TodoForegroundService::class.java)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotifications()
        registerCalendarObserver()
        return START_STICKY
    }

    private fun updateNotifications() {
        val todos = CalendarTodoSource.getTodos(this)
        val nm = getSystemService(NotificationManager::class.java)

        val summary = NotificationHelper.buildSummaryNotification(this, todos)
        startForeground(NotificationHelper.NOTIFICATION_ID_SUMMARY, summary)

        val newIds = mutableSetOf<Int>()
        todos.forEach { todo ->
            val id = NotificationHelper.getNotificationIdForTodo(todo.id)
            nm.cancel(id)
            nm.notify(id, NotificationHelper.buildTodoNotification(this, todo))
            newIds.add(id)
        }

        (activeNotifIds - newIds).forEach { nm.cancel(it) }
        activeNotifIds.clear()
        activeNotifIds.addAll(newIds)
    }

    private fun registerCalendarObserver() {
        if (calendarObserver != null) return
        val handler = Handler(Looper.getMainLooper())
        calendarObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                updateNotifications()
            }
        }.also { observer ->
            contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )
        }
    }

    override fun onDestroy() {
        calendarObserver?.let { contentResolver.unregisterContentObserver(it) }
        calendarObserver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
