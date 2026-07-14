package tachiyomi.data.entry

import kotlinx.coroutines.flow.Flow
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import tachiyomi.data.DatabaseHandler

class ViewerSettingOverrideRepositoryImpl(
    private val handler: DatabaseHandler,
) : ViewerSettingOverrideRepository {
    override suspend fun get(entryId: Long, settingId: ViewerSettingId): ViewerSettingOverride? {
        return handler.awaitOneOrNull {
            viewer_setting_overridesQueries.get(
                entryId = entryId,
                providerId = settingId.providerId,
                settingKey = settingId.key,
                mapper = ::map,
            )
        }
    }

    override fun observe(entryId: Long, settingId: ViewerSettingId): Flow<ViewerSettingOverride?> {
        return handler.subscribeToOneOrNull {
            viewer_setting_overridesQueries.get(
                entryId = entryId,
                providerId = settingId.providerId,
                settingKey = settingId.key,
                mapper = ::map,
            )
        }
    }

    override suspend fun getByEntryId(entryId: Long): List<ViewerSettingOverride> {
        return handler.awaitList {
            viewer_setting_overridesQueries.getByEntryId(entryId, ::map)
        }
    }

    override suspend fun upsert(override: ViewerSettingOverride) {
        require(override.encodedValue.length <= MAX_VALUE_LENGTH) { "Viewer setting override is too large" }
        handler.await {
            viewer_setting_overridesQueries.upsert(
                entryId = override.entryId,
                providerId = override.settingId.providerId,
                settingKey = override.settingId.key,
                encodedValue = override.encodedValue,
                updatedAt = override.updatedAt,
            )
        }
    }

    override suspend fun delete(entryId: Long, settingId: ViewerSettingId) {
        handler.await {
            viewer_setting_overridesQueries.delete(
                entryId = entryId,
                providerId = settingId.providerId,
                settingKey = settingId.key,
            )
        }
    }

    override suspend fun replaceForEntry(entryId: Long, overrides: List<ViewerSettingOverride>) {
        require(overrides.all { it.entryId == entryId }) { "Override entry IDs must match the replaced entry" }
        handler.await(inTransaction = true) {
            viewer_setting_overridesQueries.deleteByEntryId(entryId)
            overrides.forEach { override ->
                require(override.encodedValue.length <= MAX_VALUE_LENGTH) { "Viewer setting override is too large" }
                viewer_setting_overridesQueries.upsert(
                    entryId = entryId,
                    providerId = override.settingId.providerId,
                    settingKey = override.settingId.key,
                    encodedValue = override.encodedValue,
                    updatedAt = override.updatedAt,
                )
            }
        }
    }

    override suspend fun copy(sourceEntryId: Long, targetEntryId: Long) {
        handler.await {
            viewer_setting_overridesQueries.copy(
                sourceEntryId = sourceEntryId,
                targetEntryId = targetEntryId,
            )
        }
    }

    override suspend fun deleteByProviderForProfile(providerId: String, profileId: Long) {
        handler.await {
            viewer_setting_overridesQueries.deleteByProviderForProfile(
                providerId = providerId,
                profileId = profileId,
            )
        }
    }

    private fun map(
        entryId: Long,
        providerId: String,
        settingKey: String,
        encodedValue: String,
        updatedAt: Long,
    ): ViewerSettingOverride = ViewerSettingOverride(
        entryId = entryId,
        settingId = ViewerSettingId(providerId, settingKey),
        encodedValue = encodedValue,
        updatedAt = updatedAt,
    )

    private companion object {
        const val MAX_VALUE_LENGTH = 16_384
    }
}
