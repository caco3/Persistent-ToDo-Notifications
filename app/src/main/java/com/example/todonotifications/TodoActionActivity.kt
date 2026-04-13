package com.example.todonotifications

import android.app.NotificationManager
import android.content.ContentUris
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TodoActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val todoId = intent.getStringExtra(NotificationActionReceiver.EXTRA_TODO_ID)
        val notifId = intent.getIntExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, -1)
        val isRecurring = intent.getBooleanExtra(NotificationActionReceiver.EXTRA_IS_RECURRING, false)
        val dtStart = intent.getLongExtra(NotificationActionReceiver.EXTRA_DT_START, 0L)
        val title = intent.getStringExtra(NotificationActionReceiver.EXTRA_TITLE) ?: ""

        if (todoId == null) {
            finish()
            return
        }

        showMainDialog(todoId, notifId, isRecurring, dtStart, title)
    }

    private fun showMainDialog(todoId: String, notifId: Int, isRecurring: Boolean, dtStart: Long, title: String) {
        val todo = TodoItem(id = todoId, title = title, dtStart = dtStart, isRecurring = isRecurring)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_todo_action, null)
        view.findViewById<MaterialButton>(R.id.btnDone).text = NotificationHelper.buildDoneLabel(this, todo)
        val dialog = MaterialAlertDialogBuilder(this, R.style.WideDialog)
            .setTitle(title)
            .setView(view)
            .setNegativeButton(R.string.dialog_cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
        view.findViewById<MaterialButton>(R.id.btnDone).setOnClickListener {
            dialog.dismiss()
            markAsDone(todoId, notifId, isRecurring, dtStart)
        }
        view.findViewById<MaterialButton>(R.id.btnSnooze).setOnClickListener {
            dialog.dismiss()
            showSnoozeDialog(todoId, notifId)
        }
        view.findViewById<MaterialButton>(R.id.btnOpenCalendar).setOnClickListener {
            dialog.dismiss()
            openInCalendar(todoId)
        }
        dialog.window?.setLayout(
            resources.displayMetrics.widthPixels - 10,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun openInCalendar(todoId: String) {
        startActivity(NotificationHelper.buildOpenEventIntent(this, todoId))
        finish()
    }

    private fun showSnoozeDialog(todoId: String, notifId: Int) {
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
                AppLogger.log(this, "SNOOZE  id=$todoId  duration=${options[which]}")
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

    private fun markAsDone(todoId: String, notifId: Int, isRecurring: Boolean, dtStart: Long) {
        if (isRecurring) {
            AppPreferences.setHandledUntil(this, todoId, dtStart)
            NotificationActionReceiver.scheduleNextOccurrenceAlarm(this, todoId, dtStart)
            AppLogger.log(this, "DONE (recurring)  id=$todoId  dtStart=$dtStart")
        } else {
            val uri = ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI, todoId.toLong()
            )
            contentResolver.delete(uri, null, null)
            AppLogger.log(this, "DONE (deleted)  id=$todoId")
        }
        if (notifId != -1) {
            getSystemService(NotificationManager::class.java).cancel(notifId)
        }
        TodoForegroundService.startOrUpdate(this)
        finish()
    }
}
