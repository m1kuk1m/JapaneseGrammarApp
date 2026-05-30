package com.example.japanesegrammarapp.data.repository

import android.net.Uri

interface OcrRepository {
    suspend fun extractTextFromImage(uri: Uri): String
}
