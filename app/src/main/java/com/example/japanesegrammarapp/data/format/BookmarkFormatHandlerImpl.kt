package com.example.japanesegrammarapp.data.format

import com.example.japanesegrammarapp.domain.format.BookmarkFormatHandler
import com.example.japanesegrammarapp.domain.format.ParsedBookmarks
import com.example.japanesegrammarapp.domain.model.BookmarkedGrammarPointDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.ExportFormat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookmarkFormatHandlerImpl : BookmarkFormatHandler {
    override fun exportData(
        format: ExportFormat,
        words: List<BookmarkedSegmentDomain>,
        sentences: List<BookmarkedSentenceDomain>,
        grammarPoints: List<BookmarkedGrammarPointDomain>
    ): String {
        return when (format) {
            ExportFormat.JSON -> exportJson(words, sentences, grammarPoints)
            ExportFormat.CSV -> exportCsv(words, sentences, grammarPoints)
            ExportFormat.ANKI_TSV -> exportAnkiTsv(words, sentences, grammarPoints)
        }
    }

    override fun importData(data: String, format: ExportFormat): ParsedBookmarks {
        return when (format) {
            ExportFormat.JSON -> importJson(data)
            ExportFormat.CSV -> importCsv(data)
            ExportFormat.ANKI_TSV -> importAnkiTsv(data)
        }
    }

    private fun exportJson(
        words: List<BookmarkedSegmentDomain>,
        sentences: List<BookmarkedSentenceDomain>,
        grammarPoints: List<BookmarkedGrammarPointDomain>
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val root = JSONObject().apply {
            put("version", 3)
            put("exported_at", sdf.format(Date()))
            
            if (words.isNotEmpty()) {
                put("bookmarks", JSONArray().also { arr ->
                    words.forEach { b ->
                        arr.put(JSONObject().apply {
                            put("id", b.id)
                            put("recordId", b.recordId)
                            put("text", b.segmentText)
                            putOpt("surfaceForm", b.surfaceForm)
                            putOpt("reading", b.reading)
                            putOpt("partOfSpeech", b.partOfSpeech)
                            putOpt("posCategory", b.posCategory)
                            putOpt("dictionaryForm", b.dictionaryForm)
                            putOpt("dictionaryFormReading", b.dictionaryFormReading)
                            putOpt("meaning", b.meaning)
                            putOpt("inflection", b.inflection)
                            putOpt("role", b.role)
                            put("bookmarkedAt", b.bookmarkedAt)
                            put("sourceText", b.sourceText)
                            put("isArchived", b.isArchived)
                        })
                    }
                })
            }
            
            if (sentences.isNotEmpty()) {
                put("sentences", JSONArray().also { arr ->
                    sentences.forEach { s ->
                        arr.put(JSONObject().apply {
                            put("id", s.id)
                            put("recordId", s.recordId)
                            put("originalText", s.originalText)
                            putOpt("translation", s.translation)
                            putOpt("analysisResult", s.analysisResult)
                            putOpt("modelUsed", s.modelUsed)
                            put("bookmarkedAt", s.bookmarkedAt)
                        })
                    }
                })
            }
            
            if (grammarPoints.isNotEmpty()) {
                put("grammar_points", JSONArray().also { arr ->
                    grammarPoints.forEach { gp ->
                        arr.put(JSONObject().apply {
                            put("id", gp.id)
                            put("recordId", gp.recordId)
                            put("pattern", gp.pattern)
                            putOpt("explanation", gp.explanation)
                            put("bookmarkedAt", gp.bookmarkedAt)
                            put("sourceText", gp.sourceText)
                            put("isArchived", gp.isArchived)
                        })
                    }
                })
            }
        }
        return root.toString(2)
    }

    private fun importJson(json: String): ParsedBookmarks {
        val root = JSONObject(json)
        val failureReasons = mutableListOf<String>()
        val words = mutableListOf<BookmarkedSegmentDomain>()
        val sentences = mutableListOf<BookmarkedSentenceDomain>()
        val grammarPoints = mutableListOf<BookmarkedGrammarPointDomain>()

        if (root.has("bookmarks")) {
            val arr = root.getJSONArray("bookmarks")
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    words.add(BookmarkedSegmentDomain(
                        id = 0,
                        recordId = obj.optInt("recordId", -1),
                        segmentText = obj.optString("text", ""),
                        surfaceForm = obj.optStringOrNull("surfaceForm"),
                        reading = obj.optStringOrNull("reading"),
                        partOfSpeech = obj.optStringOrNull("partOfSpeech"),
                        posCategory = obj.optStringOrNull("posCategory"),
                        dictionaryForm = obj.optStringOrNull("dictionaryForm"),
                        dictionaryFormReading = obj.optStringOrNull("dictionaryFormReading"),
                        meaning = obj.optStringOrNull("meaning"),
                        inflection = obj.optStringOrNull("inflection"),
                        role = obj.optStringOrNull("role"),
                        bookmarkedAt = obj.optLong("bookmarkedAt", System.currentTimeMillis()),
                        sourceText = obj.optString("sourceText", ""),
                        isArchived = obj.optBoolean("isArchived", false)
                    ))
                } catch (e: Exception) {
                    failureReasons.add("Word import failed: ${e.message}")
                }
            }
        }

        if (root.has("sentences")) {
            val arr = root.getJSONArray("sentences")
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    sentences.add(BookmarkedSentenceDomain(
                        id = 0,
                        recordId = obj.optInt("recordId", -1),
                        originalText = obj.optString("originalText", ""),
                        translation = obj.optStringOrNull("translation"),
                        analysisResult = obj.optStringOrNull("analysisResult"),
                        modelUsed = obj.optStringOrNull("modelUsed"),
                        bookmarkedAt = obj.optLong("bookmarkedAt", System.currentTimeMillis())
                    ))
                } catch (e: Exception) {
                    failureReasons.add("Sentence import failed: ${e.message}")
                }
            }
        }

        if (root.has("grammar_points")) {
            val arr = root.getJSONArray("grammar_points")
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    grammarPoints.add(BookmarkedGrammarPointDomain(
                        id = 0,
                        recordId = obj.optInt("recordId", -1),
                        pattern = obj.optString("pattern", ""),
                        explanation = obj.optStringOrNull("explanation"),
                        bookmarkedAt = obj.optLong("bookmarkedAt", System.currentTimeMillis()),
                        sourceText = obj.optString("sourceText", ""),
                        isArchived = obj.optBoolean("isArchived", false)
                    ))
                } catch (e: Exception) {
                    failureReasons.add("Grammar point import failed: ${e.message}")
                }
            }
        }

        return ParsedBookmarks(words, sentences, grammarPoints, failureReasons)
    }

    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun exportCsv(
        words: List<BookmarkedSegmentDomain>,
        sentences: List<BookmarkedSentenceDomain>,
        grammarPoints: List<BookmarkedGrammarPointDomain>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("Type,Front,Back,Extra1,Extra2\n")
        words.forEach { w ->
            sb.append("Word,")
            sb.append(escapeCsv(w.segmentText)).append(",")
            sb.append(escapeCsv(w.meaning)).append(",")
            sb.append(escapeCsv(w.reading)).append(",")
            sb.append(escapeCsv(w.sourceText)).append("\n")
        }
        sentences.forEach { s ->
            sb.append("Sentence,")
            sb.append(escapeCsv(s.originalText)).append(",")
            sb.append(escapeCsv(s.translation)).append(",")
            sb.append(escapeCsv("")).append(",")
            sb.append(escapeCsv("")).append("\n")
        }
        grammarPoints.forEach { gp ->
            sb.append("Grammar,")
            sb.append(escapeCsv(gp.pattern)).append(",")
            sb.append(escapeCsv(gp.explanation)).append(",")
            sb.append(escapeCsv(gp.sourceText)).append(",")
            sb.append(escapeCsv("")).append("\n")
        }
        return sb.toString()
    }

    private fun importCsv(csv: String): ParsedBookmarks {
        val failureReasons = mutableListOf<String>()
        val words = mutableListOf<BookmarkedSegmentDomain>()
        val sentences = mutableListOf<BookmarkedSentenceDomain>()
        val grammarPoints = mutableListOf<BookmarkedGrammarPointDomain>()

        val lines = csv.lines()
        if (lines.isEmpty()) return ParsedBookmarks(emptyList(), emptyList(), emptyList(), listOf("Empty CSV"))
        
        // Skip header if matches "Type,Front,Back..."
        val startIdx = if (lines[0].startsWith("Type,")) 1 else 0

        for (i in startIdx until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            try {
                // A very basic CSV parser that might fail on complex multiline values,
                // but good enough for this context as specified "CSV Format: Include a 'Type' column followed by relevant fields."
                val parts = line.split(",") // Simplification, standard parser preferable if complex but let's stick to simple since no external library
                if (parts.isNotEmpty()) {
                    val type = parts[0]
                    when (type) {
                        "Word" -> {
                            words.add(BookmarkedSegmentDomain(
                                id = 0, recordId = -1,
                                segmentText = parts.getOrNull(1)?.removeSurrounding("\"") ?: "",
                                meaning = parts.getOrNull(2)?.removeSurrounding("\""),
                                reading = parts.getOrNull(3)?.removeSurrounding("\""),
                                sourceText = parts.getOrNull(4)?.removeSurrounding("\"") ?: "",
                                bookmarkedAt = System.currentTimeMillis()
                            ))
                        }
                        "Sentence" -> {
                            sentences.add(BookmarkedSentenceDomain(
                                id = 0, recordId = -1,
                                originalText = parts.getOrNull(1)?.removeSurrounding("\"") ?: "",
                                translation = parts.getOrNull(2)?.removeSurrounding("\""),
                                analysisResult = null,
                                modelUsed = null,
                                bookmarkedAt = System.currentTimeMillis()
                            ))
                        }
                        "Grammar" -> {
                            grammarPoints.add(BookmarkedGrammarPointDomain(
                                id = 0, recordId = -1,
                                pattern = parts.getOrNull(1)?.removeSurrounding("\"") ?: "",
                                explanation = parts.getOrNull(2)?.removeSurrounding("\""),
                                sourceText = parts.getOrNull(3)?.removeSurrounding("\"") ?: "",
                                bookmarkedAt = System.currentTimeMillis()
                            ))
                        }
                        else -> {
                            failureReasons.add("Unknown type in CSV on line ${i + 1}: $type")
                        }
                    }
                }
            } catch (e: Exception) {
                failureReasons.add("Failed to parse CSV line ${i + 1}: ${e.message}")
            }
        }
        return ParsedBookmarks(words, sentences, grammarPoints, failureReasons)
    }

    private fun exportAnkiTsv(
        words: List<BookmarkedSegmentDomain>,
        sentences: List<BookmarkedSentenceDomain>,
        grammarPoints: List<BookmarkedGrammarPointDomain>
    ): String {
        val sb = java.lang.StringBuilder()
        // Anki TSV typically has no header
        words.forEach { w ->
            val front = w.segmentText.replace("\t", " ").replace("\n", " ")
            val back = (w.reading ?: "") + "<br>" + (w.meaning ?: "") + "<br>" + w.sourceText.replace("\t", " ").replace("\n", " ")
            sb.append(front).append("\t").append(back).append("\tWord\n")
        }
        sentences.forEach { s ->
            val front = s.originalText.replace("\t", " ").replace("\n", " ")
            val back = (s.translation ?: "").replace("\t", " ").replace("\n", " ")
            sb.append(front).append("\t").append(back).append("\tSentence\n")
        }
        grammarPoints.forEach { gp ->
            val front = gp.pattern.replace("\t", " ").replace("\n", " ")
            val back = (gp.explanation ?: "") + "<br>" + gp.sourceText.replace("\t", " ").replace("\n", " ")
            sb.append(front).append("\t").append(back).append("\tGrammar\n")
        }
        return sb.toString()
    }

    private fun importAnkiTsv(tsv: String): ParsedBookmarks {
        val failureReasons = mutableListOf<String>()
        val words = mutableListOf<BookmarkedSegmentDomain>()
        val sentences = mutableListOf<BookmarkedSentenceDomain>()
        val grammarPoints = mutableListOf<BookmarkedGrammarPointDomain>()

        val lines = tsv.lines()
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val parts = line.split("\t")
            if (parts.size >= 2) {
                val front = parts[0]
                val back = parts[1]
                val type = parts.getOrNull(2) ?: "Word"
                
                when (type) {
                    "Word" -> {
                        words.add(BookmarkedSegmentDomain(
                            id = 0, recordId = -1,
                            segmentText = front,
                            meaning = back.replace("<br>", "\n"),
                            bookmarkedAt = System.currentTimeMillis()
                        ))
                    }
                    "Sentence" -> {
                        sentences.add(BookmarkedSentenceDomain(
                            id = 0, recordId = -1,
                            originalText = front,
                            translation = back.replace("<br>", "\n"),
                            analysisResult = null,
                            modelUsed = null,
                            bookmarkedAt = System.currentTimeMillis()
                        ))
                    }
                    "Grammar" -> {
                        grammarPoints.add(BookmarkedGrammarPointDomain(
                            id = 0, recordId = -1,
                            pattern = front,
                            explanation = back.replace("<br>", "\n"),
                            bookmarkedAt = System.currentTimeMillis()
                        ))
                    }
                }
            } else {
                failureReasons.add("Failed to parse TSV line ${i + 1}")
            }
        }

        return ParsedBookmarks(words, sentences, grammarPoints, failureReasons)
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
    }
}
