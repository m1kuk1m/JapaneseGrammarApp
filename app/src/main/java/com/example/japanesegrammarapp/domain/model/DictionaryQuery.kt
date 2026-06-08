package com.example.japanesegrammarapp.domain.model

fun WordSegment.dictionaryQueryWord(): String {
    var query = dictionaryFormReading?.takeIf { it.isNotBlank() }
        ?: dictionaryForm?.takeIf { it.isNotBlank() }
        ?: text
        ?: ""

    query = if (partOfSpeech == "形容動詞" || partOfSpeech == "形状詞") {
        query.removeSuffix("だ").removeSuffix("な").removeSuffix("に").removeSuffix("で")
    } else {
        query.removeSuffix("だ")
    }

    if (query.endsWith("する") && query != "する") {
        query = query.removeSuffix("する")
    }

    return query
}
