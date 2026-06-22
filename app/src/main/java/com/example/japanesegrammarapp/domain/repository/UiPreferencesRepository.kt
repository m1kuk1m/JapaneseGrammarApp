package com.example.japanesegrammarapp.domain.repository

interface UiPreferencesRepository {
    fun getFloatingActionBallX(defaultValue: Float): Float
    fun getFloatingActionBallY(defaultValue: Float): Float
    fun saveFloatingActionBallPosition(x: Float, y: Float)
    fun getFloatingActionBallMode(defaultValue: String): String
    fun saveFloatingActionBallMode(mode: String)
    fun getLastDictionary(defaultValue: String): String
    fun saveLastDictionary(dictionary: String)
    fun getCropInteraction(defaultValue: String): String
    fun saveCropInteraction(mode: String)
}
