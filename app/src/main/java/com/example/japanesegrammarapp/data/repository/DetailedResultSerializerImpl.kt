package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.repository.DetailedResultSerializer
import com.google.gson.Gson
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailedResultSerializerImpl @Inject constructor(
    private val gson: Gson
) : DetailedResultSerializer {

    private val parsedCache = ConcurrentHashMap<String, DetailedAnalysisResult>()

    override fun toJson(result: DetailedAnalysisResult): String {
        return gson.toJson(result)
    }

    override fun fromJson(json: String?): DetailedAnalysisResult? {
        if (json.isNullOrBlank()) return null
        val cached = parsedCache[json]
        if (cached != null) return cached

        return try {
            val clean = cleanMarkdownJson(json)
            val parsed = gson.fromJson(clean, DetailedAnalysisResult::class.java)
            if (parsed != null) {
                if (parsedCache.size > 50) {
                    parsedCache.clear()
                }
                parsedCache[json] = parsed
            }
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanMarkdownJson(rawJson: String): String {
        var cleanJson = rawJson.trim()
        if (cleanJson.startsWith("```")) {
            val firstNewLine = cleanJson.indexOf('\n')
            cleanJson = if (firstNewLine != -1) {
                cleanJson.substring(firstNewLine).trim()
            } else {
                cleanJson.removePrefix("```").trim()
            }
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```").trim()
        }
        return cleanJson
    }
}
