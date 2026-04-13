package com.example.todonotifications

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val LOG_FILE = "todo_actions.log"
    private const val MAX_BYTES = 200_000L

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun log(context: Context, message: String) {
        val file = logFile(context)
        if (file.length() > MAX_BYTES) rotate(file)
        file.appendText("${fmt.format(Date())}  $message\n")
    }

    fun logFile(context: Context): File = File(context.filesDir, LOG_FILE)

    private fun rotate(file: File) {
        val lines = file.readLines()
        val keep = lines.drop(lines.size / 2)
        file.writeText(keep.joinToString("\n") + "\n")
    }
}
