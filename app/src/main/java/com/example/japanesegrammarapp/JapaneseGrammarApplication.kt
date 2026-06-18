package com.example.japanesegrammarapp

import android.app.Application
import com.example.japanesegrammarapp.domain.ApplicationScope
import com.example.japanesegrammarapp.utils.AppLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class JapaneseGrammarApplication : Application() {
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this, applicationScope)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                AppLogger.logCrashSync(throwable)
            } catch (e: Exception) {
                // Ignore exception to prevent infinite loop
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
