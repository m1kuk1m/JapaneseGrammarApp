package com.example.japanesegrammarapp.domain.repository

import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult

interface DetailedResultSerializer {
    fun toJson(result: DetailedAnalysisResult): String
    fun fromJson(json: String?): DetailedAnalysisResult?
}
