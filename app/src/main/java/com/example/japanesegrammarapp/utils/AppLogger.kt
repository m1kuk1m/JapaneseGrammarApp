package com.example.japanesegrammarapp.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    fun d(tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val log = "[$time] D/$tag: $message"
        android.util.Log.d(tag, message)
        _logs.value = (_logs.value + log).takeLast(100) // keep last 100
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val log = "[$time] E/$tag: $message" + (throwable?.let { "\n${it.stackTraceToString()}" } ?: "")
        android.util.Log.e(tag, message, throwable)
        _logs.value = (_logs.value + log).takeLast(100)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
