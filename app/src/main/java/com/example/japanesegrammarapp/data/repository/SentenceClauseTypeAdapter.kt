package com.example.japanesegrammarapp.data.repository

import com.example.japanesegrammarapp.domain.model.SentenceClause
import com.google.gson.*
import java.lang.reflect.Type

class SentenceClauseTypeAdapter : JsonDeserializer<SentenceClause>, JsonSerializer<SentenceClause> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SentenceClause {
        if (json.isJsonArray) {
            val arr = json.asJsonArray
            val index = if (arr.size() > 0 && !arr.get(0).isJsonNull) arr.get(0).asInt else null
            val role = if (arr.size() > 1 && !arr.get(1).isJsonNull) arr.get(1).asString else null
            val text = if (arr.size() > 2 && !arr.get(2).isJsonNull) arr.get(2).asString else null
            val explanation = if (arr.size() > 3 && !arr.get(3).isJsonNull) arr.get(3).asString else null
            return SentenceClause(
                index = index,
                role = role,
                text = text,
                explanation = explanation
            )
        } else if (json.isJsonObject) {
            val obj = json.asJsonObject
            return SentenceClause(
                index = obj.get("index")?.takeIf { !it.isJsonNull }?.asInt,
                role = obj.get("role")?.takeIf { !it.isJsonNull }?.asString,
                text = obj.get("text")?.takeIf { !it.isJsonNull }?.asString,
                explanation = obj.get("explanation")?.takeIf { !it.isJsonNull }?.asString
            )
        }
        return SentenceClause()
    }

    override fun serialize(
        src: SentenceClause,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val obj = JsonObject()
        obj.addProperty("index", src.index)
        obj.addProperty("role", src.role)
        obj.addProperty("text", src.text)
        obj.addProperty("explanation", src.explanation)
        return obj
    }
}
