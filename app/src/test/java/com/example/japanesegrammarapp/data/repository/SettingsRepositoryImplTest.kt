package com.example.japanesegrammarapp.data.repository

import android.content.SharedPreferences
import com.example.japanesegrammarapp.domain.model.EndpointUrlValidator
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.domain.model.LlmEndpoint
import com.example.japanesegrammarapp.ui.SettingsUiState
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryImplTest {
    @Test
    fun settingsUiStateDefaultsMatchAiDirectMode() {
        val state = SettingsUiState()

        assertFalse(state.useOcr)
        assertEquals("faithful", state.imageTokenizerMode)
    }

    @Test
    fun legacyProviderKeyAndUrlAreMigratedToDefaultEndpoint() {
        val standardPrefs = TestSharedPreferences().apply {
            edit()
                .putString("Gemini_url", "https://legacy.example/v1")
                .apply()
        }
        val securePrefs = TestSharedPreferences().apply {
            edit()
                .putString("Gemini_key", "legacy-secret")
                .apply()
        }
        val repository = newRepository(standardPrefs, securePrefs)

        val endpoints = repository.getEndpoints("Gemini")

        assertEquals(1, endpoints.size)
        assertEquals("default_gemini", endpoints.single().id)
        assertEquals("https://legacy.example/v1", endpoints.single().baseUrl)
        assertEquals("legacy-secret", repository.getApiKeyForEndpoint("default_gemini"))
        assertEquals("legacy-secret", repository.getApiKey("Gemini"))
    }

    @Test
    fun saveEndpointWithoutApiKeyStoresDraftAsDisabled() {
        val repository = newRepository()

        assertTrue(
            repository.saveEndpoint(
                LlmEndpoint(
                    id = "draft",
                    provider = "DeepSeek",
                    name = "Draft",
                    baseUrl = "https://draft.example",
                    enabled = true
                ),
                ""
            )
        )

        val saved = repository.getEndpoints("DeepSeek").first { it.id == "draft" }
        assertFalse(saved.enabled)
        assertEquals("", repository.getApiKeyForEndpoint("draft"))
    }

    @Test
    fun clearingEndpointApiKeyDisablesEndpointAndRemovesItFromApiConfigs() {
        val repository = newRepository()
        val endpoint = LlmEndpoint(
            id = "clear-key",
            provider = "Qwen",
            name = "Clear Key",
            baseUrl = "https://qwen.example",
            enabled = true
        )
        assertTrue(repository.saveEndpoint(endpoint, "secret"))
        assertEquals(1, repository.buildLlmApiConfigs("Qwen", "qwen-test").size)

        assertTrue(repository.saveEndpoint(endpoint, ""))

        val saved = repository.getEndpoints("Qwen").first { it.id == "clear-key" }
        assertFalse(saved.enabled)
        assertEquals("", repository.getApiKeyForEndpoint("clear-key"))
        assertTrue(repository.buildLlmApiConfigs("Qwen", "qwen-test").isEmpty())
    }

    @Test
    fun endpointUrlValidatorAcceptsOnlyHttpAndHttpsUrls() {
        assertTrue(EndpointUrlValidator.isValidHttpUrl("https://api.example.com/v1"))
        assertTrue(EndpointUrlValidator.isValidHttpUrl(" http://localhost:8080 "))

        assertFalse(EndpointUrlValidator.isValidHttpUrl(""))
        assertFalse(EndpointUrlValidator.isValidHttpUrl("api.example.com/v1"))
        assertFalse(EndpointUrlValidator.isValidHttpUrl("ftp://api.example.com"))
        assertFalse(EndpointUrlValidator.isValidHttpUrl("https://"))
    }

    @Test
    fun saveEndpointAddsAndUpdatesEndpointWithApiKey() {
        val repository = newRepository()
        val endpoint = LlmEndpoint(
            id = "backup-1",
            provider = "DeepSeek",
            name = "Backup",
            baseUrl = "https://backup.example",
            priority = 2,
            weight = 3
        )

        assertTrue(repository.saveEndpoint(endpoint, "endpoint-secret"))
        assertEquals("endpoint-secret", repository.getApiKeyForEndpoint("backup-1"))

        val updated = endpoint.copy(
            name = "Backup Updated",
            baseUrl = "https://backup-updated.example",
            priority = -5,
            weight = 0
        )
        assertTrue(repository.saveEndpoint(updated))

        val saved = repository.getEndpoints("DeepSeek").first { it.id == "backup-1" }
        assertEquals("Backup Updated", saved.name)
        assertEquals("https://backup-updated.example", saved.baseUrl)
        assertEquals(0, saved.priority)
        assertEquals(1, saved.weight)
        assertEquals("endpoint-secret", repository.getApiKeyForEndpoint("backup-1"))
    }

    @Test
    fun deleteEndpointRemovesKeyAndRestoresDefaultWhenLastEndpointIsDeleted() {
        val repository = newRepository()
        val endpoint = LlmEndpoint(
            id = "only-endpoint",
            provider = "Qwen",
            name = "Only",
            baseUrl = "https://only.example"
        )
        assertTrue(repository.saveEndpoint(endpoint, "to-remove"))

        assertTrue(repository.deleteEndpoint("Qwen", "only-endpoint"))

        assertEquals("", repository.getApiKeyForEndpoint("only-endpoint"))
        val endpoints = repository.getEndpoints("Qwen")
        assertEquals(1, endpoints.size)
        assertEquals("default_qwen", endpoints.single().id)
        assertEquals(LlmConfig.defaultUrls.getValue("Qwen"), endpoints.single().baseUrl)
    }

    @Test
    fun saveApiKeyReturnsFalseAndDoesNotCacheWhenSecurePrefsCommitFails() {
        val repository = newRepository(
            securePrefs = TestSharedPreferences(failCommitsForKeys = setOf("llm_endpoint_default_gemini_key"))
        )

        assertFalse(repository.saveApiKey("Gemini", "new-secret"))

        assertEquals("", repository.getApiKeyForEndpoint("default_gemini"))
        assertEquals("", repository.getApiKey("Gemini"))
    }

    @Test
    fun buildLlmApiConfigsUsesEnabledEndpointWithKeyAndSkipsCooldownEndpoint() {
        val repository = newRepository()
        assertTrue(
            repository.saveEndpoint(
                LlmEndpoint(
                    id = "ready",
                    provider = "OpenAI Compatible",
                    name = "Ready",
                    baseUrl = "https://ready.example",
                    priority = 1
                ),
                "ready-key"
            )
        )
        assertTrue(
            repository.saveEndpoint(
                LlmEndpoint(
                    id = "cooldown",
                    provider = "OpenAI Compatible",
                    name = "Cooldown",
                    baseUrl = "https://cooldown.example",
                    cooldownUntilMs = Long.MAX_VALUE
                ),
                "cooldown-key"
            )
        )

        val configs = repository.buildLlmApiConfigs("OpenAI Compatible", "gpt-test")

        assertEquals(1, configs.size)
        assertEquals("ready", configs.single().endpointId)
        assertEquals("https://ready.example", configs.single().url)
        assertEquals("ready-key", configs.single().key)
    }

    private fun newRepository(
        standardPrefs: TestSharedPreferences = TestSharedPreferences(),
        securePrefs: TestSharedPreferences = TestSharedPreferences()
    ): SettingsRepositoryImpl {
        return SettingsRepositoryImpl(
            settingPrefs = standardPrefs,
            securePrefs = securePrefs,
            gson = Gson(),
            applicationScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }
}

private class TestSharedPreferences(
    private val failCommitsForKeys: Set<String> = emptySet()
) : SharedPreferences {
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
            if (pending.keys.any { it in failCommitsForKeys }) {
                return false
            }
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
                listeners.forEach { it.onSharedPreferenceChanged(this@TestSharedPreferences, key) }
            }
        }
    }
}
