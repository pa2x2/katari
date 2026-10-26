package eu.kanade.tachiyomi.source.lnreader.settings

import android.content.Context
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.source.entry.EntryPreferenceScreen
import eu.kanade.tachiyomi.source.lnreader.runtime.asArray
import eu.kanade.tachiyomi.source.lnreader.runtime.asBoolean
import eu.kanade.tachiyomi.source.lnreader.runtime.asObject
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import eu.kanade.tachiyomi.source.lnreader.runtime.text
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * One of a plugin's `pluginSettings`, of LNReader's four types; a setting without a type is a text, as in LNReader.
 * [default] is the definition's value, what the plugin reads until the user changes it.
 */
internal sealed class LnReaderSetting(val key: String, val label: String, val default: JsonElement) {
    class Switch(key: String, label: String, default: Boolean) : LnReaderSetting(key, label, JsonPrimitive(default))

    class Text(key: String, label: String, default: String) : LnReaderSetting(key, label, JsonPrimitive(default))

    class Select(key: String, label: String, default: String, val options: List<Pair<String, String>>) :
        LnReaderSetting(key, label, JsonPrimitive(default))

    class CheckboxGroup(key: String, label: String, default: List<String>, val options: List<Pair<String, String>>) :
        LnReaderSetting(key, label, JsonArray(default.map(::JsonPrimitive)))
}

/** The settings of [definitions] Katari can show; one of another type is left out. */
internal fun lnReaderSettings(definitions: JsonObject?): List<LnReaderSetting> =
    definitions.orEmpty().mapNotNull { (key, definition) ->
        val fields = definition.asObject() ?: return@mapNotNull null
        val label = fields.text("label") ?: key
        val value = fields["value"]
        when (fields.text("type") ?: "Text") {
            "Switch" -> LnReaderSetting.Switch(key, label, value.asBoolean() == true)
            "Text" -> LnReaderSetting.Text(key, label, value.asText().orEmpty())
            "Select" -> LnReaderSetting.Select(key, label, value.asText().orEmpty(), fields.options())
            "CheckboxGroup" -> LnReaderSetting.CheckboxGroup(
                key,
                label,
                value.asArray().mapNotNull(JsonElement::asText),
                fields.options(),
            )
            else -> null
        }
    }

/** Adds [settings] to the screen, each keeping its value in [store]. */
internal fun EntryPreferenceScreen.addLnReaderSettings(settings: List<LnReaderSetting>, store: LnReaderSettingStore) {
    settings.forEach { setting ->
        addPreference(
            setting.preference(context).apply {
                key = setting.key
                title = setting.label
                preferenceDataStore = store
            },
        )
    }
}

private fun LnReaderSetting.preference(context: Context): Preference = when (this) {
    is LnReaderSetting.Switch -> SwitchPreferenceCompat(context).apply { setDefaultValue(default.asBoolean()) }
    is LnReaderSetting.Text -> EditTextPreference(context).apply {
        setDefaultValue(default.asText())
        dialogTitle = label
        summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
    }
    is LnReaderSetting.Select -> ListPreference(context).apply {
        entries = options.map { it.first }.toTypedArray()
        entryValues = options.map { it.second }.toTypedArray()
        setDefaultValue(default.asText())
        dialogTitle = label
        summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }
    is LnReaderSetting.CheckboxGroup -> MultiSelectListPreference(context).apply {
        entries = options.map { it.first }.toTypedArray()
        entryValues = options.map { it.second }.toTypedArray()
        setDefaultValue(default.asArray().mapNotNull(JsonElement::asText).toSet())
        dialogTitle = label
    }
}

// Label to value
private fun JsonObject.options(): List<Pair<String, String>> = get("options").asArray().mapNotNull { option ->
    val fields = option.asObject() ?: return@mapNotNull null
    val value = fields.text("value") ?: return@mapNotNull null
    (fields.text("label") ?: value) to value
}
