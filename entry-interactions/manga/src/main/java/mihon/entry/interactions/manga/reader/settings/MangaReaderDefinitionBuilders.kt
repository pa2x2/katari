package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.ViewerSettingCodec
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import tachiyomi.core.common.preference.Preference

private const val PROVIDER_ID = MangaReaderSettings.PROVIDER_ID

internal fun profileBoolean(key: String, preference: Preference<Boolean>) = definition(
    key = key,
    preference = preference,
    codec = ViewerSettingCodecs.Boolean,
    scope = ViewerSettingScope.PROFILE_ONLY,
)

internal fun entryBoolean(key: String, preference: Preference<Boolean>) = definition(
    key = key,
    preference = preference,
    codec = ViewerSettingCodecs.Boolean,
    scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
)

internal fun profileInt(
    key: String,
    preference: Preference<Int>,
    validate: (Int) -> Boolean = { true },
) = definition(
    key = key,
    preference = preference,
    codec = ViewerSettingCodecs.Int,
    scope = ViewerSettingScope.PROFILE_ONLY,
    validate = validate,
)

internal fun entryInt(
    key: String,
    preference: Preference<Int>,
    validate: (Int) -> Boolean = { true },
) = definition(
    key = key,
    preference = preference,
    codec = ViewerSettingCodecs.Int,
    scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
    validate = validate,
)

internal fun <T> profileValue(
    key: String,
    preference: Preference<T>,
    codec: ViewerSettingCodec<T>,
    validate: (T) -> Boolean = { true },
) = definition(
    key = key,
    preference = preference,
    codec = codec,
    scope = ViewerSettingScope.PROFILE_ONLY,
    validate = validate,
)

internal fun <T> entryValue(
    key: String,
    preference: Preference<T>,
    codec: ViewerSettingCodec<T>,
    validate: (T) -> Boolean = { true },
) = definition(
    key = key,
    preference = preference,
    codec = codec,
    scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
    validate = validate,
)

internal fun <T> definition(
    key: String,
    preference: Preference<T>,
    codec: ViewerSettingCodec<T>,
    scope: ViewerSettingScope,
    validate: (T) -> Boolean = { true },
) = ViewerSettingDefinition(
    id = ViewerSettingId(PROVIDER_ID, key),
    scope = scope,
    processorDefault = preference.defaultValue(),
    profilePreference = preference,
    codec = codec,
    validate = validate,
)

internal fun <T : Enum<T>> enumCodec(values: List<T>) = ViewerSettingCodecs.codec<T>(
    encode = Enum<T>::name,
    decode = { encoded -> values.firstOrNull { it.name == encoded } },
)

internal fun readingModeSetCodec() = ViewerSettingCodecs.codec<Set<ReadingMode>>(
    encode = { values -> values.map(ReadingMode::name).sorted().joinToString(",") },
    decode = { encoded ->
        if (encoded.isEmpty()) {
            emptySet()
        } else {
            encoded.split(',').map { name ->
                ReadingMode.entries.firstOrNull { it.name == name } ?: return@codec null
            }.toSet()
        }
    },
)
