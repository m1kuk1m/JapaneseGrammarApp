package com.example.japanesegrammarapp.data.repository

import android.content.SharedPreferences
import com.example.japanesegrammarapp.di.StandardPrefs
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiPreferencesRepositoryImpl @Inject constructor(
    @StandardPrefs private val prefs: SharedPreferences
) : UiPreferencesRepository {
    override fun getFloatingActionBallX(defaultValue: Float): Float =
        prefs.getFloat(KEY_FAB_X, defaultValue)

    override fun getFloatingActionBallY(defaultValue: Float): Float =
        prefs.getFloat(KEY_FAB_Y, defaultValue)

    override fun saveFloatingActionBallPosition(x: Float, y: Float) {
        prefs.edit()
            .putFloat(KEY_FAB_X, x)
            .putFloat(KEY_FAB_Y, y)
            .apply()
    }

    override fun getFloatingActionBallMode(defaultValue: String): String =
        prefs.getString(KEY_FAB_MODE, defaultValue) ?: defaultValue

    override fun saveFloatingActionBallMode(mode: String) {
        prefs.edit().putString(KEY_FAB_MODE, mode).apply()
    }

    override fun getLastDictionary(defaultValue: String): String =
        prefs.getString(KEY_LAST_DICTIONARY, defaultValue) ?: defaultValue

    override fun saveLastDictionary(dictionary: String) {
        prefs.edit().putString(KEY_LAST_DICTIONARY, dictionary).apply()
    }
    override fun getCropInteraction(defaultValue: String): String =
        prefs.getString(KEY_CROP_INTERACTION, defaultValue) ?: defaultValue

    override fun saveCropInteraction(mode: String) {
        prefs.edit().putString(KEY_CROP_INTERACTION, mode).apply()
    }

    private companion object {
        const val KEY_FAB_X = "fab_x"
        const val KEY_FAB_Y = "fab_y"
        const val KEY_FAB_MODE = "fab_mode"
        const val KEY_LAST_DICTIONARY = "last_dict"
        const val KEY_CROP_INTERACTION = "crop_interaction"
    }
}
