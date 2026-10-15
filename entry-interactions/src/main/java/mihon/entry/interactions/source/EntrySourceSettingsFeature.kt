package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import tachiyomi.domain.source.service.SourceManager

internal data class SourceSettingsContext(val installed: Boolean, val configurable: Boolean)

internal class DefaultEntrySourceSettingsFeature(
    private val sourceManager: SourceManager,
) : EntrySourceSettingsFeature {

    override fun resolve(sourceId: Long): EntrySourceSettingsResolution {
        val source = sourceManager.get(sourceId)
        val configurable = source as? ConfigurableSource
        if (source == null) return EntrySourceSettingsResolution.Missing(sourceId)
        if (configurable == null) return EntrySourceSettingsResolution.Unsupported(sourceId)

        return runCatching {
            EntrySourceSettingsResolution.Available(
                sourceId = sourceId,
                preferences = configurable.getSourcePreferences(),
                populateScreen = configurable::setupPreferenceScreen,
            )
        }.getOrElse { EntrySourceSettingsResolution.Failed(sourceId, it) }
    }

    override fun supportedSourceIds(): List<Long> {
        return sourceManager.getAll()
            .filterIsInstance<ConfigurableSource>()
            .map { it.id }
            .sorted()
    }
}
