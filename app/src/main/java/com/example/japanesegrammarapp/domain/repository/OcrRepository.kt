package com.example.japanesegrammarapp.domain.repository

import android.net.Uri

interface OcrRepository {
    suspend fun extractTextFromImage(uri: Uri): String
}
