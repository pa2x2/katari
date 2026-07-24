package mihon.entry.viewer.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.core.common.preference.Preference

enum class ViewerSettingScope {
    PROFILE_ONLY,
    PROFILE_WITH_ENTRY_OVERRIDE,
}

enum class ViewerSettingSource {
    PROCESSOR_DEFAULT,
    PROFILE,
    ENTRY,
}

enum class ViewerSettingsCategory {
    READER,
    PLAYER,
}

data class ViewerSettingId(
    val providerId: String,
    val key: String,
) {
    init {
        require(providerId.isNotBlank()) { "Viewer settings provider ID must not be blank" }
        require(providerId.length <= MAX_ID_LENGTH) { "Viewer settings provider ID is too long" }
        require(key.isNotBlank()) { "Viewer setting key must not be blank" }
        require(key.length <= MAX_ID_LENGTH) { "Viewer setting key is too long" }
    }

    private companion object {
        const val MAX_ID_LENGTH = 200
    }
}

data class ResolvedViewerSetting<T>(
    val effectiveValue: T,
    val source: ViewerSettingSource,
    val processorDefault: T,
    val profileValue: T?,
    val entryOverride: T?,
    val invalidProfileValue: Boolean = false,
    val invalidEntryOverride: Boolean = false,
)

interface ViewerSettingCodec<T> {
    fun encode(value: T): String
    fun decode(value: String): T?
}

class ViewerSettingDefinition<T>(
    val id: ViewerSettingId,
    val scope: ViewerSettingScope,
    val processorDefault: T,
    val profilePreference: Preference<T>,
    val codec: ViewerSettingCodec<T>,
    val validate: (T) -> Boolean = { true },
) {
    init {
        require(validate(processorDefault)) { "Invalid processor default for $id" }
        require(validate(profilePreference.defaultValue())) { "Invalid profile default for $id" }
    }
}

interface ViewerSettingBinding<T> {
    val definition: ViewerSettingDefinition<T>
    val entryId: Long?
    val state: StateFlow<ResolvedViewerSetting<T>>

    fun resolveProfile(): ResolvedViewerSetting<T>
    fun setProfileValue(value: T)
    fun resetProfileValue()
    suspend fun setEntryOverride(value: T)
    suspend fun clearEntryOverride()
}

interface ViewerSettingBinder {
    fun <T> bind(definition: ViewerSettingDefinition<T>, entryId: Long? = null): ViewerSettingBinding<T>
    suspend fun <T> resolve(definition: ViewerSettingDefinition<T>, entryId: Long? = null): ResolvedViewerSetting<T>
}

interface ViewerSettingsProvider {
    val id: String
    val category: ViewerSettingsCategory
    val displayName: String
    val description: String?
        get() = null
    val origin: String?
        get() = null
    val settings: List<ViewerSettingDefinition<*>>
}

data class ViewerSettingOverride(
    val entryId: Long,
    val settingId: ViewerSettingId,
    val encodedValue: String,
    val updatedAt: Long,
)

interface ViewerSettingOverrideRepository {
    suspend fun get(entryId: Long, settingId: ViewerSettingId): ViewerSettingOverride?
    fun observe(entryId: Long, settingId: ViewerSettingId): Flow<ViewerSettingOverride?>
    suspend fun getByEntryId(entryId: Long): List<ViewerSettingOverride>
    suspend fun upsert(override: ViewerSettingOverride)
    suspend fun delete(entryId: Long, settingId: ViewerSettingId)
    suspend fun deleteByProviderForProfile(providerId: String, profileId: Long)
}
