package com.example.japanesegrammarapp

import android.app.Application
import com.example.japanesegrammarapp.network.JapaneseSegmenter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class JapaneseGrammarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-warm the Kuromoji Tokenizer in a background thread to avoid lazy-loading lag during first click
        CoroutineScope(Dispatchers.IO).launch {
            try {
                JapaneseSegmenter.segmentAndCombine("初期化")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
