package com.example.japanesegrammarapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.japanesegrammarapp.vision.OcrHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : OcrRepository {

    override suspend fun extractTextFromImage(uri: Uri): String {
        return OcrHelper().extractTextFromUri(context, uri)
    }
}
