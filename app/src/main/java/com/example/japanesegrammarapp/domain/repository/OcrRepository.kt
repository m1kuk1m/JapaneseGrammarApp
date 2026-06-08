package com.example.japanesegrammarapp.domain.repository

interface OcrRepository {
    suspend fun extractTextFromImage(imageUri: String): String
}