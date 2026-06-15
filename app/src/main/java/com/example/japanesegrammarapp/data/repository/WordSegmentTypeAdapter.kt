package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.model.WordSegment
import com.google.gson.*
import java.lang.reflect.Type

class WordSegmentTypeAdapter : JsonDeserializer<WordSegment>, JsonSerializer<WordSegment> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): WordSegment {
        if (json.isJsonArray) {
            val arr = json.asJsonArray
            val text = if (arr.size() > 0 && !arr.get(0).isJsonNull) arr.get(0).asString else null
            val reading = if (arr.size() > 1 && !arr.get(1).isJsonNull) arr.get(1).asString else null
            val partOfSpeech = if (arr.size() > 2 && !arr.get(2).isJsonNull) arr.get(2).asString else null
            val posCategory = if (arr.size() > 3 && !arr.get(3).isJsonNull) arr.get(3).asString else null
            val dictionaryForm = if (arr.size() > 4 && !arr.get(4).isJsonNull) arr.get(4).asString else null
            val dictionaryFormReading = if (arr.size() > 5 && !arr.get(5).isJsonNull) arr.get(5).asString else null
            val meaning = if (arr.size() > 6 && !arr.get(6).isJsonNull) arr.get(6).asString else null
            val inflection = if (arr.size() > 7 && !arr.get(7).isJsonNull) arr.get(7).asString else null
            val role = if (arr.size() > 8 && !arr.get(8).isJsonNull) arr.get(8).asString else null
            return WordSegment(
                text = text,
                reading = reading,
                partOfSpeech = partOfSpeech,
                posCategory = posCategory,
                dictionaryForm = dictionaryForm,
                dictionaryFormReading = dictionaryFormReading,
                meaning = meaning,
                inflection = inflection,
                role = role
            )
        } else if (json.isJsonObject) {
            val obj = json.asJsonObject
            return WordSegment(
                text = obj.get("text")?.takeIf { !it.isJsonNull }?.asString,
                reading = obj.get("reading")?.takeIf { !it.isJsonNull }?.asString,
                partOfSpeech = obj.get("partOfSpeech")?.takeIf { !it.isJsonNull }?.asString,
                posCategory = obj.get("posCategory")?.takeIf { !it.isJsonNull }?.asString,
                dictionaryForm = obj.get("dictionaryForm")?.takeIf { !it.isJsonNull }?.asString,
                dictionaryFormReading = obj.get("dictionaryFormReading")?.takeIf { !it.isJsonNull }?.asString,
                meaning = obj.get("meaning")?.takeIf { !it.isJsonNull }?.asString,
                inflection = obj.get("inflection")?.takeIf { !it.isJsonNull }?.asString,
                role = obj.get("role")?.takeIf { !it.isJsonNull }?.asString
            )
        }
        return WordSegment()
    }

    override fun serialize(
        src: WordSegment,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val obj = JsonObject()
        obj.addProperty("text", src.text)
        obj.addProperty("reading", src.reading)
        obj.addProperty("partOfSpeech", src.partOfSpeech)
        obj.addProperty("posCategory", src.posCategory)
        obj.addProperty("dictionaryForm", src.dictionaryForm)
        obj.addProperty("dictionaryFormReading", src.dictionaryFormReading)
        obj.addProperty("meaning", src.meaning)
        obj.addProperty("inflection", src.inflection)
        obj.addProperty("role", src.role)
        return obj
    }
}
