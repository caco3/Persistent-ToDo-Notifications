package com.example.todonotifications

data class TodoItem(
    val id: String,
    val title: String,
    val dtStart: Long = 0L,
    val isRecurring: Boolean = false
)
