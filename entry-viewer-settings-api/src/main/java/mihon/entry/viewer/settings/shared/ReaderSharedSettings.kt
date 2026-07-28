package mihon.entry.viewer.settings

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

sealed interface ReaderSharedSettingAvailability {
    data object Available : ReaderSharedSettingAvailability

    data class Disabled(
        val reason: ReaderSharedSettingText,
    ) : ReaderSharedSettingAvailability
}

class ReaderSharedToggleSetting(
    val id: ReaderSharedSettingId,
    val title: ReaderSharedSettingText,
    val summary: ReaderSharedSettingText,
    val preference: Preference<Boolean>,
    val defaultValue: Boolean,
    val requiredCapabilities: Set<ReaderCapabilityId>,
    val availabilityChanges: Flow<Unit> = emptyFlow(),
    val resolveAvailability: suspend () -> ReaderSharedSettingAvailability,
) {
    init {
        require(requiredCapabilities.isNotEmpty()) {
            "Shared reader setting ${id.value} must require at least one reader capability"
        }
        require(preference.defaultValue() == defaultValue) {
            "Shared reader setting ${id.value} default does not match its preference"
        }
    }

    fun isApplicable(capabilities: Set<ReaderCapabilityId>): Boolean =
        capabilities.containsAll(requiredCapabilities)

    fun reset() {
        preference.delete()
    }
}

interface ReaderSharedSettingsProvider {
    /** Capabilities which at least one installed processor can potentially provide. */
    val potentialCapabilities: Set<ReaderCapabilityId>
    val settings: List<ReaderSharedToggleSetting>
}

class ReaderSharedSettingsRegistry(
    providers: Collection<ReaderSharedSettingsProvider>,
) {
    private val providers = providers.toList()
    private val settingsById: Map<ReaderSharedSettingId, ReaderSharedToggleSetting>

    init {
        val allSettings = this.providers.flatMap(ReaderSharedSettingsProvider::settings)
        val duplicates = allSettings
            .groupingBy(ReaderSharedToggleSetting::id)
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicates.isEmpty()) {
            "Duplicate shared reader setting IDs: ${duplicates.map { it.value }.sorted()}"
        }
        settingsById = allSettings.associateBy(ReaderSharedToggleSetting::id)
    }

    /** Settings discoverable globally because an installed reader can potentially satisfy them. */
    fun globalSettings(): List<ReaderSharedToggleSetting> {
        return providers
            .flatMap { provider ->
                provider.settings.filter { setting ->
                    setting.isApplicable(provider.potentialCapabilities)
                }
            }
            .distinctBy(ReaderSharedToggleSetting::id)
    }

    /** Settings applicable to the currently opened reader session. */
    fun settingsFor(capabilities: Set<ReaderCapabilityId>): List<ReaderSharedToggleSetting> {
        return settingsById.values.filter { it.isApplicable(capabilities) }
    }
}
