package com.example.japanesegrammarapp.utils

import com.example.japanesegrammarapp.domain.model.PromptPreset
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Serialises and deserialises [PromptPreset] lists to/from a versioned JSON format.
 *
 * Export format:
 * ```json
 * {
 *   "version": 1,
 *   "exportedAt": "2026-07-02T17:00:00+08:00",
 *   "presets": [
 *     {
 *       "name": "My Preset",
 *       "prompts": {
 *         "prompt_translation": "...",
 *         ...
 *       }
 *     }
 *   ]
 * }
 * ```
 *
 * Note: IDs are intentionally omitted from the export – new UUIDs are
 * assigned on import so that presets from different devices never collide.
 */
object PromptPresetExporter {

    private const val CURRENT_VERSION = 1

    // ── Export ───────────────────────────────────────────────────────────────

    /**
     * Converts [presets] to a JSON string suitable for writing to a file.
     *
     * Only presets whose [PromptPreset.prompts] map is non-empty are written
     * (empty presets carry no information and would just clutter the file).
     */
    fun exportToJson(presets: List<PromptPreset>): String {
        val root = JSONObject().apply {
            put("version", CURRENT_VERSION)
            put("exportedAt", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            val presetsArray = JSONArray()
            for (preset in presets) {
                if (preset.prompts.isEmpty() && preset.id != PromptPreset.DEFAULT_PRESET_ID) continue
                val presetObj = JSONObject().apply {
                    put("name", preset.name)
                    val promptsObj = JSONObject()
                    preset.prompts.forEach { (key, value) ->
                        if (value.isNotBlank()) promptsObj.put(key, value)
                    }
                    put("prompts", promptsObj)
                }
                presetsArray.put(presetObj)
            }
            put("presets", presetsArray)
        }
        return root.toString(2) // pretty-print with 2-space indent
    }

    // ── Import ───────────────────────────────────────────────────────────────

    /**
     * Parses [json] and returns a list of [PromptPreset] objects ready for saving.
     *
     * Each preset receives a freshly generated UUID as its [PromptPreset.id].
     * The "default" preset (name == "default") is assigned
     * [PromptPreset.DEFAULT_PRESET_ID] so it merges into the built-in slot.
     *
     * @return [Result.success] with the parsed presets, or [Result.failure]
     *         with a descriptive [ImportException] on any parse / version error.
     */
    fun importFromJson(json: String): Result<List<PromptPreset>> = runCatching {
        val root = JSONObject(json)

        val version = root.optInt("version", -1)
        if (version < 1 || version > CURRENT_VERSION) {
            throw ImportException("Unsupported file version: $version (expected 1–$CURRENT_VERSION)")
        }

        val presetsArray = root.optJSONArray("presets")
            ?: throw ImportException("Missing 'presets' array in JSON")

        val result = mutableListOf<PromptPreset>()
        for (i in 0 until presetsArray.length()) {
            val obj = presetsArray.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue

            val promptsObj = obj.optJSONObject("prompts") ?: JSONObject()
            val prompts = mutableMapOf<String, String>()
            promptsObj.keys().forEach { key -> prompts[key] = promptsObj.getString(key) }

            val id = if (name.equals("default", ignoreCase = true)) {
                PromptPreset.DEFAULT_PRESET_ID
            } else {
                UUID.randomUUID().toString()
            }

            result += PromptPreset(id = id, name = name, prompts = prompts)
        }

        if (result.isEmpty()) throw ImportException("File contains no valid presets")
        result
    }

    // ── Exception ────────────────────────────────────────────────────────────

    class ImportException(message: String) : Exception(message)
}
