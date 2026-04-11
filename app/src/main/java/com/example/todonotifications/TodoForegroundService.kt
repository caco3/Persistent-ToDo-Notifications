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
    private val handler = Handler(Looper.getMainLooper())
    private val pendingNotifRunnables = mutableListOf<Runnable>()
    private var batchStartMs = 0L

    private val calendarUpdateRunnable = Runnable { updateNotifications() }

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            repostMissing()
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    companion object {
        private const val CALENDAR_DEBOUNCE_MS = 500L
        private const val WATCHDOG_MS = 3_000L
        private const val NOTIF_STAGGER_MS = 250L

        fun startOrUpdate(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TodoForegroundService::class.java)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.NOTIFICATION_ID_SUMMARY,
            NotificationHelper.buildSummaryNotification(this, emptyList())
        )
        updateNotifications()
        registerCalendarObserver()
        startWatchdog()
        return START_STICKY
    }

    private fun startWatchdog() {
        handler.removeCallbacks(watchdogRunnable)
        handler.postDelayed(watchdogRunnable, WATCHDOG_MS)
    }

    private fun repostMissing() {
        if (activeNotifIds.isEmpty()) return
        val batchDurationMs = activeNotifIds.size * NOTIF_STAGGER_MS + 1_000L
        if (System.currentTimeMillis() - batchStartMs < batchDurationMs) return
        val nm = getSystemService(NotificationManager::class.java)
        val visibleIds = nm.activeNotifications.map { it.id }.toSet()
        if (activeNotifIds.all { it in visibleIds }) return
        updateNotifications()
    }

    private fun updateNotifications() {
        batchStartMs = System.currentTimeMillis()
        val todos = if (AppPreferences.getDemoMode(this)) CalendarTodoSource.getDummyTodos()
                    else CalendarTodoSource.getTodos(this)
        val nm = getSystemService(NotificationManager::class.java)

        startForeground(
            NotificationHelper.NOTIFICATION_ID_SUMMARY,
            NotificationHelper.buildSummaryNotification(this, todos)
        )

        pendingNotifRunnables.forEach { handler.removeCallbacks(it) }
        pendingNotifRunnables.clear()

        val newIds = todos.map { NotificationHelper.getNotificationIdForTodo(it.id) }.toSet()
        (activeNotifIds - newIds).forEach { nm.cancel(it) }
        activeNotifIds.clear()
        activeNotifIds.addAll(newIds)

        todos.forEachIndexed { index, todo ->
            val id = NotificationHelper.getNotificationIdForTodo(todo.id)
            val runnable = Runnable { nm.notify(id, NotificationHelper.buildTodoNotification(this, todo)) }
            pendingNotifRunnables.add(runnable)
            handler.postDelayed(runnable, index * NOTIF_STAGGER_MS)
        }
    }

    private fun registerCalendarObserver() {
        if (calendarObserver != null) return
        calendarObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                handler.removeCallbacks(calendarUpdateRunnable)
                handler.postDelayed(calendarUpdateRunnable, CALENDAR_DEBOUNCE_MS)
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
        handler.removeCallbacks(calendarUpdateRunnable)
        handler.removeCallbacks(watchdogRunnable)
        pendingNotifRunnables.forEach { handler.removeCallbacks(it) }
        pendingNotifRunnables.clear()
        calendarObserver?.let { contentResolver.unregisterContentObserver(it) }
        calendarObserver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
