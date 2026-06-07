package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.repository.AppLogWriter
import com.example.japanesegrammarapp.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogWriterImpl @Inject constructor() : AppLogWriter {
    override fun error(tag: String, message: String, throwable: Throwable?) {
        AppLogger.e(tag, message, throwable)
    }
}
