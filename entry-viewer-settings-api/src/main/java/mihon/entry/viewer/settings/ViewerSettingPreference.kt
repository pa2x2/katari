package mihon.entry.viewer.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference

/**
 * Adapts a profile binding to existing preference controls without duplicating resolution logic.
 */
fun <T> ViewerSettingBinding<T>.asProfilePreference(): Preference<T> {
    require(entryId == null) { "A profile preference cannot be created from an entry binding" }
    return ViewerSettingProfilePreference(this)
}

/**
 * Returns every setting from one viewer surface to its processor default.
 *
 * Profile values are removed for all definitions. When [entryId] is provided,
 * definitions that support entry overrides also clear the override for that
 * entry, allowing it to inherit the reset profile value.
 */
suspend fun ViewerSettingBinder.resetSettings(
    provider: ViewerSettingsProvider,
    entryId: Long? = null,
) {
    provider.settings.forEach { definition ->
        definition.profilePreference.delete()
        if (entryId != null && definition.scope == ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE) {
            clearEntryOverride(definition, entryId)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private suspend fun ViewerSettingBinder.clearEntryOverride(
    definition: ViewerSettingDefinition<*>,
    entryId: Long,
) {
    bind(definition as ViewerSettingDefinition<Any?>, entryId).clearEntryOverride()
}

private class ViewerSettingProfilePreference<T>(
    private val binding: ViewerSettingBinding<T>,
) : Preference<T> {
    override fun key(): String = binding.definition.profilePreference.key()

    override fun get(): T = binding.resolveProfile().effectiveValue

    override fun set(value: T) = binding.setProfileValue(value)

    override fun isSet(): Boolean = binding.definition.profilePreference.isSet()

    override fun delete() = binding.resetProfileValue()

    override fun defaultValue(): T = binding.definition.processorDefault

    override fun changes(): Flow<T> = binding.state
        .map { setting -> setting.effectiveValue }
        .distinctUntilChanged()

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(scope, SharingStarted.Eagerly, get())
    }
}
