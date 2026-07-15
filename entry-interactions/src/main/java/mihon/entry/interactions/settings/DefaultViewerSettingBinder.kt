package mihon.entry.interactions.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import mihon.entry.viewer.settings.ResolvedViewerSetting
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingSource

internal class DefaultViewerSettingBinder(
    private val overrideRepository: ViewerSettingOverrideRepository,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewerSettingBinder {
    override fun <T> bind(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ViewerSettingBinding<T> = DefaultViewerSettingBinding(
        definition = definition,
        entryId = entryId,
        overrideRepository = overrideRepository,
        scope = scope,
        now = now,
    )

    override suspend fun <T> resolve(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ResolvedViewerSetting<T> {
        val override = when {
            definition.scope != ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE -> null
            entryId == null -> null
            else -> overrideRepository.get(entryId, definition.id)
        }
        return resolve(
            definition = definition,
            profileValue = definition.profilePreference.get().takeIf { definition.profilePreference.isSet() },
            override = override,
        )
    }
}

private class DefaultViewerSettingBinding<T>(
    override val definition: ViewerSettingDefinition<T>,
    override val entryId: Long?,
    private val overrideRepository: ViewerSettingOverrideRepository,
    scope: CoroutineScope,
    private val now: () -> Long,
) : ViewerSettingBinding<T> {
    private val profileValues = definition.profilePreference.changes()
        .onStart { emit(definition.profilePreference.get()) }
        .map { value -> ProfileLayer(value.takeIf { definition.profilePreference.isSet() }) }

    private val overrides = when {
        definition.scope != ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE -> flowOf(null)
        entryId == null -> flowOf(null)
        else -> overrideRepository.observe(entryId, definition.id)
    }

    override val state = combine(profileValues, overrides) { profile, override ->
        resolve(definition, profile.value, override)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = resolve(
            definition = definition,
            profileValue = definition.profilePreference.get().takeIf { definition.profilePreference.isSet() },
            override = null,
        ),
    )

    override fun resolveProfile(): ResolvedViewerSetting<T> {
        return resolve(
            definition = definition,
            profileValue = definition.profilePreference.get().takeIf { definition.profilePreference.isSet() },
            override = null,
        )
    }

    override fun setProfileValue(value: T) {
        require(definition.validate(value)) { "Invalid profile value for ${definition.id}" }
        definition.profilePreference.set(value)
    }

    override fun resetProfileValue() {
        definition.profilePreference.delete()
    }

    override suspend fun setEntryOverride(value: T) {
        val targetEntryId = requireOverrideEntryId()
        require(definition.validate(value)) { "Invalid entry override for ${definition.id}" }
        overrideRepository.upsert(
            ViewerSettingOverride(
                entryId = targetEntryId,
                settingId = definition.id,
                encodedValue = definition.codec.encode(value),
                updatedAt = now(),
            ),
        )
    }

    override suspend fun clearEntryOverride() {
        overrideRepository.delete(requireOverrideEntryId(), definition.id)
    }

    private fun requireOverrideEntryId(): Long {
        check(definition.scope == ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE) {
            "${definition.id} does not support entry overrides"
        }
        return checkNotNull(entryId) { "An entry ID is required to update ${definition.id}" }
    }
}

private data class ProfileLayer<T>(val value: T?)

private fun <T> resolve(
    definition: ViewerSettingDefinition<T>,
    profileValue: T?,
    override: ViewerSettingOverride?,
): ResolvedViewerSetting<T> {
    val validProfile = profileValue?.takeIf(definition.validate)
    val decodedOverride = override?.encodedValue?.let(definition.codec::decode)
    val validOverride = decodedOverride?.takeIf(definition.validate)

    return when {
        validOverride != null -> ResolvedViewerSetting(
            effectiveValue = validOverride,
            source = ViewerSettingSource.ENTRY,
            processorDefault = definition.processorDefault,
            profileValue = validProfile,
            entryOverride = validOverride,
            invalidProfileValue = profileValue != null && validProfile == null,
        )
        validProfile != null -> ResolvedViewerSetting(
            effectiveValue = validProfile,
            source = ViewerSettingSource.PROFILE,
            processorDefault = definition.processorDefault,
            profileValue = validProfile,
            entryOverride = null,
            invalidEntryOverride = override != null,
        )
        else -> ResolvedViewerSetting(
            effectiveValue = definition.processorDefault,
            source = ViewerSettingSource.PROCESSOR_DEFAULT,
            processorDefault = definition.processorDefault,
            profileValue = null,
            entryOverride = null,
            invalidProfileValue = profileValue != null,
            invalidEntryOverride = override != null,
        )
    }
}
