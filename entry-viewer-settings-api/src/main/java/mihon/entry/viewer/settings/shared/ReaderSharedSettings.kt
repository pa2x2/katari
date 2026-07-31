package mihon.entry.viewer.settings.shared

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import tachiyomi.core.common.preference.Preference

@JvmInline
value class ReaderCapabilityId(
    val value: String,
) {
    init {
        require(value.matches(Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*"""))) {
            "Invalid reader capability id: '$value'"
        }
    }
}

object StandardReaderCapabilities {
    val StableTextSelection = ReaderCapabilityId("text-selection.stable")
    val SelectionAnchoring = ReaderCapabilityId("text-selection.anchor")
    val NextChapterPreparation = ReaderCapabilityId("chapter.next-preparation")
}

@JvmInline
value class ReaderSharedSettingId(
    val value: String,
) {
    init {
        require(value.matches(Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*"""))) {
            "Invalid shared reader setting id: '$value'"
        }
    }
}

fun interface ReaderSharedSettingText {
    fun resolve(context: Context): String
}

class ReaderSharedSettingAction(
    val label: ReaderSharedSettingText,
    private val perform: (Context) -> Unit,
) {
    fun perform(context: Context) {
        perform.invoke(context)
    }
}

sealed interface ReaderSharedSettingAvailability {
    data object Available : ReaderSharedSettingAvailability

    data class Disabled(
        val reason: ReaderSharedSettingText,
        val action: ReaderSharedSettingAction? = null,
    ) : ReaderSharedSettingAvailability
}

class ReaderSharedToggleSetting(
    val id: ReaderSharedSettingId,
    val title: ReaderSharedSettingText,
    val summary: ReaderSharedSettingText,
    val preferenceBinding: ReaderSharedTogglePreferenceBinding,
    val defaultValue: Boolean,
    val requiredCapabilities: Set<ReaderCapabilityId>,
    val availabilityChanges: Flow<Unit> = emptyFlow(),
    val resolveAvailability: suspend () -> ReaderSharedSettingAvailability,
) {
    init {
        require(requiredCapabilities.isNotEmpty()) {
            "Shared reader setting ${id.value} must require at least one reader capability"
        }
        require(preferenceBinding.preferences.all { it.defaultValue() == defaultValue }) {
            "Shared reader setting ${id.value} default does not match all of its preferences"
        }
    }

    fun isApplicable(capabilities: Set<ReaderCapabilityId>): Boolean =
        capabilities.containsAll(requiredCapabilities)
}

sealed interface ReaderSharedTogglePreferenceBinding {
    val preferences: Collection<Preference<Boolean>>

    data class Global(
        val preference: Preference<Boolean>,
    ) : ReaderSharedTogglePreferenceBinding {
        override val preferences = listOf(preference)
    }

    data class PerSettingsSurface(
        val preferencesBySurface: Map<String, Preference<Boolean>>,
    ) : ReaderSharedTogglePreferenceBinding {
        init {
            require(preferencesBySurface.isNotEmpty()) {
                "A per-surface shared reader setting must bind at least one settings surface"
            }
            require(preferencesBySurface.keys.none(String::isBlank)) {
                "Per-surface shared reader setting surface IDs must not be blank"
            }
        }

        override val preferences = preferencesBySurface.values
    }
}

class ResolvedReaderSharedToggleSetting internal constructor(
    val declaration: ReaderSharedToggleSetting,
    val preference: Preference<Boolean>,
    val settingsSurfaceId: String?,
) {
    val id: ReaderSharedSettingId
        get() = declaration.id
    val title: ReaderSharedSettingText
        get() = declaration.title
    val summary: ReaderSharedSettingText
        get() = declaration.summary
    val availabilityChanges: Flow<Unit>
        get() = declaration.availabilityChanges

    suspend fun resolveAvailability(): ReaderSharedSettingAvailability = declaration.resolveAvailability()

    fun reset() {
        preference.delete()
    }
}

interface ReaderSharedSettingsProvider {
    /** Capabilities which at least one installed processor can potentially provide. */
    val potentialCapabilities: Set<ReaderCapabilityId>

    /**
     * Potential capabilities associated with each app-level viewer settings surface.
     *
     * This lets the common app projection place shared declarations on every capable surface without making
     * individual screen implementations know about those declarations.
     */
    val potentialCapabilitiesBySettingsSurface: Map<String, Set<ReaderCapabilityId>>
        get() = emptyMap()

    val settings: List<ReaderSharedToggleSetting>
}

class ReaderSharedSettingsRegistry(
    providers: Collection<ReaderSharedSettingsProvider>,
) {
    private val providers = providers.toList()
    init {
        val invalidSurfaceIds = this.providers
            .flatMap { it.potentialCapabilitiesBySettingsSurface.keys }
            .filter(String::isBlank)
        require(invalidSurfaceIds.isEmpty()) {
            "Shared reader settings surface IDs must not be blank"
        }

        val allSettings = this.providers.flatMap(ReaderSharedSettingsProvider::settings)
        val duplicates = allSettings
            .groupingBy(ReaderSharedToggleSetting::id)
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicates.isEmpty()) {
            "Duplicate shared reader setting IDs: ${duplicates.map { it.value }.sorted()}"
        }
    }

    /** Globally bound settings discoverable on the Readers root when an installed reader can satisfy them. */
    fun rootSettings(): List<ResolvedReaderSharedToggleSetting> {
        return providers
            .flatMap { provider ->
                provider.settings.mapNotNull { setting ->
                    setting.resolveForRoot(provider.potentialCapabilities)
                }
            }
            .distinctBy(ResolvedReaderSharedToggleSetting::id)
    }

    /** Settings applicable to an app-level viewer settings surface. */
    fun settingsForSurface(surfaceId: String): List<ResolvedReaderSharedToggleSetting> {
        require(surfaceId.isNotBlank()) { "Viewer settings surface ID must not be blank" }
        return providers.flatMap { provider ->
            val capabilities = provider.potentialCapabilitiesBySettingsSurface[surfaceId].orEmpty()
            provider.settings.mapNotNull { setting -> setting.resolve(capabilities, surfaceId) }
        }
    }

    /** Settings applicable to the currently opened reader session. */
    fun settingsFor(
        capabilities: Set<ReaderCapabilityId>,
        settingsSurfaceId: String,
    ): List<ResolvedReaderSharedToggleSetting> {
        require(settingsSurfaceId.isNotBlank()) { "Viewer settings surface ID must not be blank" }
        return providers.flatMap { provider ->
            provider.settings.mapNotNull { setting -> setting.resolve(capabilities, settingsSurfaceId) }
        }
    }
}

private fun ReaderSharedToggleSetting.resolveForRoot(
    potentialCapabilities: Set<ReaderCapabilityId>,
): ResolvedReaderSharedToggleSetting? {
    if (!isApplicable(potentialCapabilities)) return null
    return when (val binding = preferenceBinding) {
        is ReaderSharedTogglePreferenceBinding.Global ->
            ResolvedReaderSharedToggleSetting(this, binding.preference, null)
        is ReaderSharedTogglePreferenceBinding.PerSettingsSurface -> null
    }
}

private fun ReaderSharedToggleSetting.resolve(
    capabilities: Set<ReaderCapabilityId>,
    settingsSurfaceId: String,
): ResolvedReaderSharedToggleSetting? {
    if (!isApplicable(capabilities)) return null
    val preference = when (val binding = preferenceBinding) {
        is ReaderSharedTogglePreferenceBinding.Global -> binding.preference
        is ReaderSharedTogglePreferenceBinding.PerSettingsSurface ->
            binding.preferencesBySurface[settingsSurfaceId] ?: return null
    }
    return ResolvedReaderSharedToggleSetting(this, preference, settingsSurfaceId)
}
