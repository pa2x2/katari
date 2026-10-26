package eu.kanade.tachiyomi.source.lnreader.catalogue

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterRole
import eu.kanade.tachiyomi.source.lnreader.runtime.asArray
import eu.kanade.tachiyomi.source.lnreader.runtime.asBoolean
import eu.kanade.tachiyomi.source.lnreader.runtime.asObject
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import eu.kanade.tachiyomi.source.lnreader.runtime.text
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/*
 * A plugin's filters, declared as `{key: {label, type, value, options}}`, as Katari's filters. A search sends each
 * definition back with its value replaced: LNReader sends changed filters as `{type, value}` and unchanged ones as the
 * definition itself, and plugins read only `value`, so a definition with the value in it satisfies both. Types Katari
 * has no control for are left out of the sheet and sent back as declared.
 */

/** A filter made from the plugin's definition under [key], which gives the value to send back. */
private interface LnReaderFilter : EntryFilterMetadataProvider {
    val key: String

    fun value(): JsonElement
}

internal fun lnReaderFilterList(definitions: JsonObject?): EntryFilterList =
    EntryFilterList(definitions.orEmpty().mapNotNull { (key, definition) -> definition.asObject()?.toFilter(key) })

/** [definitions] with the values [filters] hold, for `popularNovels`' `filters`. */
internal fun lnReaderFilterValues(definitions: JsonObject, filters: List<EntryFilter<*>>): JsonObject {
    val values = filters.filterIsInstance<LnReaderFilter>().associate { it.key to it.value() }
    return JsonObject(
        definitions.mapValues { (key, definition) ->
            val value = values[key]
            if (definition is JsonObject && value != null) JsonObject(definition + ("value" to value)) else definition
        },
    )
}

private fun JsonObject.toFilter(key: String): EntryFilter<*>? {
    val label = text("label") ?: key
    val value = get("value")
    return when (text("type")) {
        "Text" -> TextFilter(key, label, value.asText().orEmpty())
        "Picker" -> {
            val options = options().ifEmpty { return null }
            val selected = value.asText()
            PickerFilter(key, label, options, options.indexOfFirst { it.value == selected }.coerceAtLeast(0))
        }
        "Switch" -> SwitchFilter(key, label, value.asBoolean() == true)
        // CheckboxGroup isn't one of LNReader's types, but a plugin declares it and reads it as Checkbox
        "Checkbox", "CheckboxGroup" -> {
            val selected = value.strings()
            val options = options().ifEmpty { return null }
            CheckboxFilter(key, label, options.map { OptionCheckBox(key, it, it.value in selected) })
        }
        "XCheckbox" -> {
            val included = value.asObject()?.get("include").strings()
            val excluded = value.asObject()?.get("exclude").strings()
            val options = options().ifEmpty { return null }
            ExcludableCheckboxFilter(
                key,
                label,
                options.map { option ->
                    val state = when (option.value) {
                        in included -> EntryFilter.TriState.STATE_INCLUDE
                        in excluded -> EntryFilter.TriState.STATE_EXCLUDE
                        else -> EntryFilter.TriState.STATE_IGNORE
                    }
                    OptionTriState(key, option, state)
                },
            )
        }
        else -> null
    }
}

private class Option(val label: String, val value: String)

private fun JsonObject.options(): List<Option> = get("options").asArray().mapNotNull { option ->
    val fields = option.asObject() ?: return@mapNotNull null
    val value = fields.text("value") ?: return@mapNotNull null
    Option(fields.text("label") ?: value, value)
}

private fun JsonElement?.strings(): Set<String> = asArray().mapNotNull { it.asText() }.toSet()

private class TextFilter(override val key: String, label: String, state: String) :
    EntryFilter.Text(label, state),
    LnReaderFilter {
    override val filterMetadata = EntryFilterMetadata(id = key)

    override fun value() = JsonPrimitive(state)
}

private class PickerFilter(override val key: String, label: String, private val options: List<Option>, state: Int) :
    EntryFilter.Select<String>(label, options.map { it.label }.toTypedArray(), state),
    LnReaderFilter {
    override val filterMetadata = EntryFilterMetadata(
        id = key,
        role = if (ORDERING.containsMatchIn(key) || ORDERING.containsMatchIn(name)) {
            EntryFilterRole.ORDERING
        } else {
            EntryFilterRole.CONSTRAINT
        },
        optionIds = options.map { it.value },
    )

    override fun value() = JsonPrimitive(options.getOrElse(state) { options.first() }.value)

    private companion object {
        val ORDERING = Regex("sort|order", RegexOption.IGNORE_CASE)
    }
}

private class SwitchFilter(override val key: String, label: String, state: Boolean) :
    EntryFilter.CheckBox(label, state),
    LnReaderFilter {
    override val filterMetadata = EntryFilterMetadata(id = key)

    override fun value() = JsonPrimitive(state)
}

private class OptionCheckBox(key: String, val option: Option, state: Boolean) :
    EntryFilter.CheckBox(option.label, state),
    EntryFilterMetadataProvider {
    override val filterMetadata = EntryFilterMetadata(id = "$key/${option.value}")
}

private class CheckboxFilter(override val key: String, label: String, options: List<OptionCheckBox>) :
    EntryFilter.Group<OptionCheckBox>(label, options),
    LnReaderFilter {
    override val filterMetadata = EntryFilterMetadata(id = key)

    override fun value() = JsonArray(state.filter { it.state }.map { JsonPrimitive(it.option.value) })
}

private class OptionTriState(key: String, val option: Option, state: Int) :
    EntryFilter.TriState(option.label, state),
    EntryFilterMetadataProvider {
    override val filterMetadata = EntryFilterMetadata(id = "$key/${option.value}")
}

private class ExcludableCheckboxFilter(override val key: String, label: String, options: List<OptionTriState>) :
    EntryFilter.Group<OptionTriState>(label, options),
    LnReaderFilter {
    override val filterMetadata = EntryFilterMetadata(id = key)

    override fun value() = buildJsonObject {
        put("include", JsonArray(state.filter { it.isIncluded() }.map { JsonPrimitive(it.option.value) }))
        put("exclude", JsonArray(state.filter { it.isExcluded() }.map { JsonPrimitive(it.option.value) }))
    }
}
