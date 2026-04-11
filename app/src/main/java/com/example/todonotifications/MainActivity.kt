package com.example.todonotifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todonotifications.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TodoAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, getString(R.string.permission_denied_message), Toast.LENGTH_LONG).show()
        }
    }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            refreshTodos()
            startNotificationService()
        } else {
            Toast.makeText(this, getString(R.string.permission_calendar_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.subtitle = "build: ${BuildConfig.GIT_HASH}"

        adapter = TodoAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        checkPermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()
        if (hasCalendarPermission()) refreshTodos()
    }

    private fun checkPermissionsAndStart() {
        if (!hasCalendarPermission()) {
            requestCalendarPermission()
            return
        }
        refreshTodos()
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        startNotificationService()
    }

    private fun hasCalendarPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCalendarPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_calendar_rationale_title)
                .setMessage(R.string.permission_calendar_rationale_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        } else {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) return
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_rationale_title)
                .setMessage(R.string.permission_rationale_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startNotificationService() {
        NotificationHelper.postTodoNotification(this)
    }

    private fun refreshTodos() {
        val todos = CalendarTodoSource.getTodos(this)
        adapter.submitList(todos)
        binding.textStatus.text = if (todos.isEmpty())
            getString(R.string.status_empty)
        else
            resources.getQuantityString(R.plurals.notification_title_count, todos.size, todos.size)
    }
}
