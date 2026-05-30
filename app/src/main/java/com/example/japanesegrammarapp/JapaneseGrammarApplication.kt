package com.example.japanesegrammarapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class JapaneseGrammarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
