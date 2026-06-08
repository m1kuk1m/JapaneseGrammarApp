package com.example.japanesegrammarapp.domain.model

enum class AnalysisStep(val labelResId: Int) {
    TOKENIZATION(com.example.japanesegrammarapp.R.string.step_segments),
    CLAUSE_ANALYSIS(com.example.japanesegrammarapp.R.string.step_clauses),
    GRAMMAR_EXPLANATION(com.example.japanesegrammarapp.R.string.step_grammar),
    DETAILED_GRAMMAR(com.example.japanesegrammarapp.R.string.step_grammar),
    GRAMMAR(com.example.japanesegrammarapp.R.string.step_grammar),
    TRANSLATION(com.example.japanesegrammarapp.R.string.step_translation),
    CLAUSES(com.example.japanesegrammarapp.R.string.step_clauses),
    SEGMENTS(com.example.japanesegrammarapp.R.string.step_segments)
}