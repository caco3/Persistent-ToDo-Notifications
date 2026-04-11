package com.example.todonotifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todonotifications.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_ADD_TODO = "com.example.todonotifications.ACTION_ADD_TODO"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var todoPrefs: TodoPreferences
    private lateinit var adapter: TodoAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            postNotification()
        } else {
            Toast.makeText(
                this,
                getString(R.string.permission_denied_message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        todoPrefs = TodoPreferences(this)

        setupRecyclerView()
        setupFab()
        setupClearCompletedButton()
        checkNotificationPermissionAndPost()

        if (intent?.action == ACTION_ADD_TODO) {
            showAddTodoDialog()
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_ADD_TODO) {
            showAddTodoDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTodos()
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onComplete = { todo ->
                todoPrefs.completeTodo(todo.id)
                refreshTodos()
                NotificationHelper.postTodoNotification(this)
            },
            onDelete = { todo ->
                todoPrefs.deleteTodo(todo.id)
                refreshTodos()
                NotificationHelper.postTodoNotification(this)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTodo.setOnClickListener {
            showAddTodoDialog()
        }
    }

    private fun setupClearCompletedButton() {
        binding.btnClearCompleted.setOnClickListener {
            todoPrefs.clearCompleted()
            refreshTodos()
            NotificationHelper.postTodoNotification(this)
        }
    }

    private fun showAddTodoDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.dialog_hint)
            setPadding(56, 32, 56, 16)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.dialog_add) { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    todoPrefs.addTodo(TodoItem(title = title))
                    refreshTodos()
                    NotificationHelper.postTodoNotification(this)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
            .also { editText.requestFocus() }
    }

    private fun checkNotificationPermissionAndPost() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> postNotification()

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.permission_rationale_title)
                        .setMessage(R.string.permission_rationale_message)
                        .setPositiveButton(R.string.permission_grant) { _, _ ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show()
                }

                else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            postNotification()
        }
    }

    private fun postNotification() {
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.postTodoNotification(this)
    }

    private fun refreshTodos() {
        val todos = todoPrefs.getTodos()
        adapter.submitList(todos.toList())

        val pending = todos.count { !it.isCompleted }
        val done = todos.count { it.isCompleted }
        binding.textStatus.text = getString(R.string.status_text, pending, done)
        binding.btnClearCompleted.isEnabled = done > 0
    }
}
