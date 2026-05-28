package com.example.japanesegrammarapp.network

import com.atilika.kuromoji.ipadic.Tokenizer

object JapaneseSegmenter {
    private val tokenizer by lazy { Tokenizer() }

    /**
     * Tokenizes Japanese text using Kuromoji IPADic and applies custom semantic rules
     * to group tokens into cohesive, natural reading chunks (文節-like segments).
     */
    fun segmentAndCombine(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val tokens = try {
            tokenizer.tokenize(text)
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of any unexpected errors, fall back to returning the whole sentence as a single block
            return listOf(text)
        }

        val result = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            var surface = token.surface ?: ""
            var pos = token.partOfSpeechLevel1 ?: ""
            var pos2 = token.partOfSpeechLevel2 ?: ""

            var j = i + 1
            while (j < tokens.size) {
                val nextToken = tokens[j]
                val nextPos = nextToken.partOfSpeechLevel1 ?: ""
                val nextPos2 = nextToken.partOfSpeechLevel2 ?: ""

                var shouldMerge = false

                // 1. Verb/Adjective + Auxiliary Verb/Suffix (e.g. 「切り捨て」 + 「た」 -> 「切り捨てた」)
                if ((pos == "動詞" || pos == "形容詞") && 
                    (nextPos == "助動詞" || nextPos.contains("接尾") || nextPos2.contains("接尾"))) {
                    shouldMerge = true
                }
                // 2. Noun + Suffix (e.g. 「事件」 + 「性」 -> 「事件性」)
                else if (pos == "名詞" && (nextPos.contains("接尾") || nextPos2.contains("接尾"))) {
                    shouldMerge = true
                }
                // 3. Adjectival Noun stem + "に" / "な" (e.g. 「安易」 + 「に」 -> 「安易に」)
                else if (pos2 == "形容動詞語幹" && (nextToken.surface == "に" || nextToken.surface == "な")) {
                    shouldMerge = true
                }
                // 3.5. Auxiliary Verb + Auxiliary Verb (e.g. Kuromoji splits 「だろう」 into 「だろ」+「う」,
                //      or 「ない」+「だろう」, etc. Merge consecutive 助動詞 tokens to form complete forms.)
                else if (pos == "助動詞" && nextPos == "助動詞") {
                    shouldMerge = true
                }
                // 4. Auxiliary Verb + Particle (e.g. 「だろう」 + 「か」 -> 「だろうか」)
                else if (pos == "助動詞" && nextPos == "助詞") {
                    shouldMerge = true
                }
                // 5. Punctuation/Symbols should merge into whatever precedes them
                else if (nextPos == "記号") {
                    shouldMerge = true
                }

                if (shouldMerge) {
                    surface += nextToken.surface ?: ""
                    pos = nextPos
                    pos2 = nextPos2
                    j++
                } else {
                    break
                }
            }

            if (surface.isNotEmpty()) {
                result.add(surface)
            }
            i = j
        }
        return result
    }
}
