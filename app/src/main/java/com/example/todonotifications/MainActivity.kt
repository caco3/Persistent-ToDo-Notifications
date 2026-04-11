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
    private var updatingFilters = false

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

        adapter = TodoAdapter { todo -> openTodoEvent(todo) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.switchShowOld.isChecked = AppPreferences.getShowOldEvents(this)
        binding.switchShowOld.setOnCheckedChangeListener { _, checked ->
            if (updatingFilters) return@setOnCheckedChangeListener
            updatingFilters = true
            if (checked) {
                binding.switchNearOnly.isChecked = false
                AppPreferences.setNearOnly(this, false)
            }
            AppPreferences.setShowOldEvents(this, checked)
            if (hasCalendarPermission()) refreshTodos(scrollToTop = true)
            startNotificationService()
            updatingFilters = false
        }

        binding.switchNearOnly.isChecked = AppPreferences.getNearOnly(this)
        binding.switchNearOnly.setOnCheckedChangeListener { _, checked ->
            if (updatingFilters) return@setOnCheckedChangeListener
            updatingFilters = true
            if (checked) {
                binding.switchShowOld.isChecked = false
                AppPreferences.setShowOldEvents(this, false)
            }
            AppPreferences.setNearOnly(this, checked)
            if (hasCalendarPermission()) refreshTodos(scrollToTop = true)
            startNotificationService()
            updatingFilters = false
        }

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

    private fun openTodoEvent(todo: TodoItem) {
        val intent = NotificationHelper.buildOpenEventIntent(this, todo.id)
        startActivity(intent)
    }

    private fun refreshTodos(scrollToTop: Boolean = false) {
        binding.progressIndicator.visibility = android.view.View.VISIBLE
        val todos = CalendarTodoSource.getTodos(this)
        adapter.submitList(todos) {
            binding.progressIndicator.visibility = android.view.View.GONE
            if (scrollToTop) binding.recyclerView.scrollToPosition(0)
        }
        binding.textStatus.text = if (todos.isEmpty())
            getString(R.string.status_empty)
        else
            resources.getQuantityString(R.plurals.notification_title_count, todos.size, todos.size) + ":"
    }
}
