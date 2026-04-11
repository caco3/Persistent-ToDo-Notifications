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

    companion object {
        fun startOrUpdate(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TodoForegroundService::class.java)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildNotification(this)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        registerCalendarObserver()
        return START_STICKY
    }

    private fun registerCalendarObserver() {
        if (calendarObserver != null) return
        val handler = Handler(Looper.getMainLooper())
        calendarObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val updated = NotificationHelper.buildNotification(this@TodoForegroundService)
                getSystemService(NotificationManager::class.java)
                    .notify(NotificationHelper.NOTIFICATION_ID, updated)
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
