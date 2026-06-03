package com.example.japanesegrammarapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JapaneseGrammarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.japanesegrammarapp.utils.AppLogger.init(this)
    }
}
