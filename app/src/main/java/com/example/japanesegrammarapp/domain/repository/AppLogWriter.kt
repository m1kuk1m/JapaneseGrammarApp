package com.example.japanesegrammarapp.domain.repository

interface AppLogWriter {
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
