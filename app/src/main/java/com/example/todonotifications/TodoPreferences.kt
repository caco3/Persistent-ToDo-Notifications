package com.example.todonotifications

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TodoPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "todo_prefs"
        private const val KEY_TODOS = "todos"
    }

    fun getTodos(): MutableList<TodoItem> {
        val json = prefs.getString(KEY_TODOS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TodoItem>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveTodos(todos: List<TodoItem>) {
        prefs.edit().putString(KEY_TODOS, gson.toJson(todos)).apply()
    }

    fun addTodo(todo: TodoItem) {
        val todos = getTodos()
        todos.add(todo)
        saveTodos(todos)
    }

    fun completeTodo(id: String) {
        val todos = getTodos()
        val index = todos.indexOfFirst { it.id == id }
        if (index != -1) {
            todos[index] = todos[index].copy(isCompleted = true)
            saveTodos(todos)
        }
    }

    fun deleteTodo(id: String) {
        val todos = getTodos()
        todos.removeAll { it.id == id }
        saveTodos(todos)
    }

    fun clearCompleted() {
        val todos = getTodos().filter { !it.isCompleted }
        saveTodos(todos)
    }
}
