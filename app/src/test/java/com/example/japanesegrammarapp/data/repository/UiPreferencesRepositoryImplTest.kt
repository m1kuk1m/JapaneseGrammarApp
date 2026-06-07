package com.example.japanesegrammarapp.data.repository

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class UiPreferencesRepositoryImplTest {
    @Test
    fun floatingActionBallPreferencesRoundTrip() {
        val repository = UiPreferencesRepositoryImpl(InMemorySharedPreferences())

        assertEquals(10f, repository.getFloatingActionBallX(10f), 0.001f)
        assertEquals(20f, repository.getFloatingActionBallY(20f), 0.001f)
        assertEquals("Text", repository.getFloatingActionBallMode("Text"))

        repository.saveFloatingActionBallPosition(42.5f, 88.25f)
        repository.saveFloatingActionBallMode("Camera")

        assertEquals(42.5f, repository.getFloatingActionBallX(10f), 0.001f)
        assertEquals(88.25f, repository.getFloatingActionBallY(20f), 0.001f)
        assertEquals("Camera", repository.getFloatingActionBallMode("Text"))
    }

    @Test
    fun lastDictionaryPreferenceRoundTrip() {
        val repository = UiPreferencesRepositoryImpl(InMemorySharedPreferences())

        assertEquals("EUDIC", repository.getLastDictionary("EUDIC"))

        repository.saveLastDictionary("MOJI")

        assertEquals("MOJI", repository.getLastDictionary("EUDIC"))
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val values = linkedMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = LinkedHashMap(values)
    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        val set = values[key] as? Set<String> ?: return defValues
        return set.toMutableSet()
    }
    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners += listener
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners -= listener
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            pending[key] = values?.toSet()
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun remove(key: String): SharedPreferences.Editor = apply {
            removals += key
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearRequested = true
        }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun applyChanges() {
            val changedKeys = linkedSetOf<String>()
            if (clearRequested) {
                changedKeys += values.keys
                values.clear()
            }
            removals.forEach { key ->
                if (values.remove(key) != null) {
                    changedKeys += key
                }
            }
            pending.forEach { (key, value) ->
                values[key] = value
                changedKeys += key
            }
            changedKeys.forEach { key ->
                listeners.forEach { it.onSharedPreferenceChanged(this@InMemorySharedPreferences, key) }
            }
        }
    }
}
