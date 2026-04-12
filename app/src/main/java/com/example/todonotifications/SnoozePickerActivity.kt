package com.example.todonotifications

import android.app.NotificationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SnoozePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val todoId = intent.getStringExtra(NotificationActionReceiver.EXTRA_TODO_ID)
        val notifId = intent.getIntExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, -1)

        if (todoId == null) {
            finish()
            return
        }

        val options = arrayOf(
            getString(R.string.snooze_1day),
            getString(R.string.snooze_3days),
            getString(R.string.snooze_1week),
            getString(R.string.snooze_1month)
        )
        val durations = longArrayOf(
            1 * 24 * 60 * 60 * 1000L,
            3 * 24 * 60 * 60 * 1000L,
            7 * 24 * 60 * 60 * 1000L,
            30 * 24 * 60 * 60 * 1000L
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.snooze_title)
            .setItems(options) { _, which ->
                AppPreferences.snoozeTodo(this, todoId, durations[which])
                NotificationActionReceiver.scheduleSnoozeWakeup(this, durations[which])
                if (notifId != -1) {
                    getSystemService(NotificationManager::class.java).cancel(notifId)
                }
                TodoForegroundService.startOrUpdate(this)
                finish()
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
