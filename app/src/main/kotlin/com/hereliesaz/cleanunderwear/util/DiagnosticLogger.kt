package com.hereliesaz.cleanunderwear.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    data class LogEntry(
        val timestamp: String,
        val message: String,
        val level: LogLevel = LogEntry.LogLevel.DEBUG
    ) {
        enum class LogLevel { INFO, DEBUG, WARN, ERROR }
    }

    fun log(message: String, level: LogEntry.LogLevel = LogEntry.LogLevel.DEBUG) {
        val entry = LogEntry(dateFormat.format(Date()), message, level)
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry) // Newest first
        if (currentLogs.size > 500) currentLogs.removeAt(currentLogs.size - 1)
        _logs.value = currentLogs
        
        // Also log to system
        when (level) {
            LogEntry.LogLevel.INFO -> android.util.Log.i("CleanUnderwear", message)
            LogEntry.LogLevel.DEBUG -> android.util.Log.d("CleanUnderwear", message)
            LogEntry.LogLevel.WARN -> android.util.Log.w("CleanUnderwear", message)
            LogEntry.LogLevel.ERROR -> android.util.Log.e("CleanUnderwear", message)
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
