package com.example.japanesegrammarapp.network

import com.atilika.kuromoji.ipadic.Tokenizer

object JapaneseSegmenter {
    private val tokenizer by lazy { Tokenizer() }

    /**
     * Tokenizes Japanese text using Kuromoji IPADic and applies 文節-based rules
     * to group tokens into natural language chunks that match how people perceive
     * and use Japanese in daily speech.
     *
     * 文節 principle: Each chunk = one independent word (自立語) + all following
     * dependent words (付属語: particles, auxiliary verbs, suffixes).
     *
     * Target output for 「ここは事件性がないからと安易に切り捨てた俺の反省点だろうか。」:
     *   ここは ｜ 事件性が ｜ ないからと ｜ 安易に ｜ 切り捨てた ｜ 俺の ｜ 反省点 ｜ だろうか。
     *
     * Target output for 「図書館で本を読んでいる。」:
     *   図書館で ｜ 本を ｜ 読んでいる。
     */
    fun segmentAndCombine(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val tokens = try {
            tokenizer.tokenize(text)
        } catch (e: Exception) {
            e.printStackTrace()
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

                // ── Rule 1: Content word + Noun suffix ──────────────────────────────────
                // e.g. 「事件」+「性」→「事件性」, 「反省」+「点」→「反省点」
                if (nextPos.contains("接尾") || nextPos2.contains("接尾")) {
                    shouldMerge = true
                }

                // ── Rule 2: Verb / Adjective + Auxiliary verb (inflection chain) ────────
                // e.g. 「切り捨て」+「た」→「切り捨てた」, 「食べ」+「たく」+「ない」→「食べたくない」
                else if ((pos == "動詞" || pos == "形容詞") && nextPos == "助動詞") {
                    shouldMerge = true
                }

                // ── Rule 3: Adjectival noun stem + 「に」/「な」 ──────────────────────
                // e.g. 「安易」+「に」→「安易に」, 「素直」+「な」→「素直な」
                else if (pos2 == "形容動詞語幹" && (nextToken.surface == "に" || nextToken.surface == "な")) {
                    shouldMerge = true
                }

                // ── Rule 4: Consecutive auxiliary verbs ──────────────────────────────────
                // Kuromoji splits compound auxiliaries: 「だろ」+「う」, 「て」+「いる」, etc.
                // Must merge them before applying downstream particle rules.
                else if (pos == "助動詞" && nextPos == "助動詞") {
                    shouldMerge = true
                }

                // ── Rule 5: Noun / Pronoun + Particle (core 文節 rule) ──────────────────
                // This is the most important bunsetsu rule: a noun head always absorbs
                // all following particles into one chunk.
                // e.g. 「ここ」+「は」→「ここは」, 「事件性」+「が」→「事件性が」,
                //      「俺」+「の」→「俺の」, 「反省点」+「を」→「反省点を」
                else if (pos == "名詞" && nextPos == "助詞") {
                    shouldMerge = true
                }

                // ── Rule 6: Auxiliary verb / Particle + Particle (chain) ─────────────────
                // Once we have an 助動詞 or 助詞 head, absorb any following 助詞 to keep
                // the whole dependent-word tail in one chunk.
                // e.g. 「ない」+「から」→「ないから」, then 「ないから」+「と」→「ないからと」
                //      「だろう」+「か」→「だろうか」
                else if ((pos == "助動詞" || pos == "助詞") && nextPos == "助詞") {
                    shouldMerge = true
                }

                // ── Rule 7: Verb/Adjective + Particle (conjunctive tail) ─────────────────
                // e.g. 「食べ」+「て」(接続助詞)→「食べて」, 「行か」+「ず」→「行かず」
                // Limit to 接続助詞 and 終助詞 to avoid pulling in unrelated particles.
                else if ((pos == "動詞" || pos == "形容詞") &&
                    nextPos == "助詞" && (nextPos2 == "接続助詞" || nextPos2 == "終助詞")) {
                    shouldMerge = true
                }

                // ── Rule 6.5: Conjunctive-particle chunk + Subsidiary verb ────────────────
                // After Rule 7 merges e.g. 「読ん」+「で」→「読んで」 (pos becomes 助詞,
                // pos2 becomes 接続助詞), the next token is a subsidiary verb (動詞-非自立)
                // like いる/おく/しまう/みる. Without this rule, 読んでいる splits into
                // 読んで｜いる。
                // e.g. 「読んで」+「いる」→「読んでいる」, 「食べて」+「おく」→「食べておく」
                else if (pos == "助詞" && pos2 == "接続助詞" && nextPos == "動詞" && nextPos2.contains("非自立")) {
                    shouldMerge = true
                }

                // ── Rule 8: Punctuation always attaches to the preceding chunk ───────────
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
