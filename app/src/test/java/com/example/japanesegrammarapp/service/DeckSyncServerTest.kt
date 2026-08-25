package com.example.japanesegrammarapp.service

import android.content.SharedPreferences
import com.example.japanesegrammarapp.data.repository.SettingsRepositoryImpl
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class DeckSyncServerTest {

    private class TestSharedPreferences : SharedPreferences {
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

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply { pending[key] = values?.toSet() }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }
            override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }
            override fun clear(): SharedPreferences.Editor = apply { clearRequested = true }

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
                    if (values.remove(key) != null) changedKeys += key
                }
                pending.forEach { (key, value) ->
                    values[key] = value
                    changedKeys += key
                }
                changedKeys.forEach { key ->
                    listeners.forEach { it.onSharedPreferenceChanged(this@TestSharedPreferences, key) }
                }
            }
        }
    }

    @Test
    fun testPingAndPairFlow() {
        val standardPrefs = TestSharedPreferences()
        val securePrefs = TestSharedPreferences()
        val repository = SettingsRepositoryImpl(
            settingPrefs = standardPrefs,
            securePrefs = securePrefs,
            gson = Gson(),
            applicationScope = CoroutineScope(Dispatchers.Unconfined)
        )

        repository.setDeckSyncPin("6721")
        repository.setDeckSyncPort(8766)
        repository.setDeckSyncEnabled(true)

        val server = DeckSyncServer(null, repository) {}
        server.start(8766)

        try {
            // Test 1: Ping
            val pingUrl = URL("http://127.0.0.1:8766/api/v1/ping")
            val connPing = pingUrl.openConnection() as HttpURLConnection
            connPing.connectTimeout = 2000
            connPing.readTimeout = 2000
            assertEquals(200, connPing.responseCode)
            val pingResp = connPing.inputStream.bufferedReader().readText()
            assertTrue(pingResp.contains("YomiLLM"))
            connPing.disconnect()

            // Test 2: Pair with correct PIN
            val pairUrl = URL("http://127.0.0.1:8766/api/v1/pair")
            val connPair = pairUrl.openConnection() as HttpURLConnection
            connPair.requestMethod = "POST"
            connPair.doOutput = true
            connPair.setRequestProperty("Content-Type", "application/json")
            connPair.connectTimeout = 2000
            connPair.readTimeout = 2000
            val body = "{\"pin\":\"6721\"}"
            connPair.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            assertEquals(200, connPair.responseCode)
            val pairResp = connPair.inputStream.bufferedReader().readText()
            assertTrue(pairResp.contains("paired"))
            assertTrue(pairResp.contains("token"))
            connPair.disconnect()

            // Test 3: Pair with wrong PIN
            val connPairWrong = pairUrl.openConnection() as HttpURLConnection
            connPairWrong.requestMethod = "POST"
            connPairWrong.doOutput = true
            connPairWrong.setRequestProperty("Content-Type", "application/json")
            connPairWrong.connectTimeout = 2000
            connPairWrong.readTimeout = 2000
            val wrongBody = "{\"pin\":\"9999\"}"
            connPairWrong.outputStream.use { it.write(wrongBody.toByteArray(Charsets.UTF_8)) }
            assertEquals(401, connPairWrong.responseCode)
            connPairWrong.disconnect()

        } finally {
            server.stop()
        }
    }
}
