package com.example.todonotifications

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.provider.CalendarContract
import android.view.Menu
import android.view.MenuItem
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
    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (hasCalendarPermission()) refreshTodos()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, getString(R.string.permission_denied_message), Toast.LENGTH_LONG).show()
        }
    }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.READ_CALENDAR] == true) {
            refreshTodos()
            NotificationHelper.createNotificationChannel(this)
            requestNotificationPermissionIfNeeded()
            requestBatteryOptimizationExemption()
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

        adapter = TodoAdapter(
            onItemClick = { todo -> openTodoEvent(todo) },
            onDeleteClick = { todo -> confirmDeleteTodo(todo) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        checkPermissionsAndStart()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
            }
            R.id.action_about -> {
                val repoUrl = getString(R.string.repo_url)
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.menu_about)
                    .setMessage("${getString(R.string.app_name)}\n\n" +
                            (if (BuildConfig.GIT_TAG.isNotEmpty()) "Version: ${BuildConfig.GIT_TAG}\n\n" else "") +
                            "Source:\n$repoUrl")
                    .setPositiveButton(R.string.about_open_repo) { _, _ ->
                        startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(repoUrl)
                            )
                        )
                    }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        if (hasCalendarPermission()) {
            contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI, true, calendarObserver
            )
            refreshTodos()
        }
    }

    override fun onPause() {
        super.onPause()
        contentResolver.unregisterContentObserver(calendarObserver)
    }

    private fun checkPermissionsAndStart() {
        if (!hasCalendarPermission()) {
            requestCalendarPermission()
            return
        }
        refreshTodos()
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        requestBatteryOptimizationExemption()
        startNotificationService()
    }

    private fun hasCalendarPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCalendarPermission() {
        val perms = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_calendar_rationale_title)
                .setMessage(R.string.permission_calendar_rationale_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    calendarPermissionLauncher.launch(perms)
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        } else {
            calendarPermissionLauncher.launch(perms)
        }
    }

    private fun hasWriteCalendarPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun confirmDeleteTodo(todo: TodoItem) {
        if (!hasWriteCalendarPermission()) {
            calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_event_title)
            .setMessage(getString(R.string.delete_event_message, todo.title))
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                val uri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI, todo.id.toLong()
                )
                contentResolver.delete(uri, null, null)
                refreshTodos()
                startNotificationService()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
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

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.battery_opt_title)
            .setMessage(R.string.battery_opt_message)
            .setPositiveButton(R.string.battery_opt_allow) { _, _ ->
                startActivity(
                    android.content.Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
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
        val todos = if (AppPreferences.getDemoMode(this)) CalendarTodoSource.getDummyTodos()
                    else CalendarTodoSource.getTodos(this)
        adapter.submitList(todos) {
            binding.progressIndicator.visibility = android.view.View.GONE
            if (scrollToTop) binding.recyclerView.scrollToPosition(0)
        }
        val daysBefore = AppPreferences.getDaysBefore(this)
        val daysAfter  = AppPreferences.getDaysAfter(this)
        binding.textStatus.text = if (todos.isEmpty())
            getString(R.string.status_empty)
        else
            resources.getQuantityString(R.plurals.notification_title_count, todos.size, todos.size) +
                    " (last $daysBefore days/next $daysAfter days):"
    }
}
