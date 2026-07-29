package mihon.entry.interactions

import android.app.Application
import coil3.ComponentRegistry
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.viewer.settings.ReaderCapabilityId
import mihon.entry.viewer.settings.ReaderSharedSettingsProvider
import uy.kohesive.injekt.api.InjektRegistrar

class EntryTypeRuntimeModule(
    val type: EntryType,
    val install: InjektRegistrar.(Application) -> EntryTypeRuntimeContribution,
)

data class EntryTypeRuntimeContribution(
    val plugin: EntryInteractionPlugin,
    val warmups: List<() -> Unit> = emptyList(),
    val imageComponentInstallers: List<EntryImageComponentInstaller> = emptyList(),
    val potentialReaderCapabilitiesBySettingsSurface: Map<String, Set<ReaderCapabilityId>> = emptyMap(),
    val sharedReaderSettingsProviderFactories: List<() -> ReaderSharedSettingsProvider> = emptyList(),
) {
    fun validate(expectedType: EntryType) {
        require(plugin.type == expectedType) {
            "Runtime module $expectedType produced plugin for ${plugin.type}"
        }
        require(potentialReaderCapabilitiesBySettingsSurface.keys.none(String::isBlank)) {
            "Runtime module $expectedType produced a blank viewer settings surface ID"
        }
    }
}

fun interface EntryImageComponentInstaller {
    fun install(builder: ComponentRegistry.Builder)
}

data class EntryImageComponentInstallers(
    val values: List<EntryImageComponentInstaller>,
)
