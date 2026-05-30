package com.example.japanesegrammarapp.data.repository

import kotlinx.coroutines.flow.StateFlow

interface TtsRepository {
    val isPlaying: StateFlow<Boolean>
    fun playText(text: String)
    fun stop()
}
