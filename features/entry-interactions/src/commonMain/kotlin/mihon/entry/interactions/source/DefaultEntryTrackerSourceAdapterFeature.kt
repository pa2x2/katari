package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.EntryImageSource
import tachiyomi.domain.source.service.SourceManager

internal class DefaultEntryTrackerSourceAdapterFeature(
    private val sourceManager: SourceManager,
    private val settings: EntrySourceSettingsFeature,
    private val home: EntrySourceHomeFeature,
) : EntryTrackerSourceAdapterFeature {

    override fun resolve(sourceId: Long): EntryTrackerSourceAdapterResolution {
        val settingsResult = settings.resolve(sourceId)
        val homeResult = home.resolve(sourceId)
        val imageSource = sourceManager.get(sourceId) as? EntryImageSource
        val applicable = settingsResult is EntrySourceSettingsResolution.Available &&
            homeResult is EntrySourceHomeResolution.Available && imageSource != null

        val failure = when {
            settingsResult is EntrySourceSettingsResolution.Failed -> settingsResult.cause
            homeResult is EntrySourceHomeResolution.Failed -> homeResult.cause
            else -> null
        }
        if (failure != null) return EntryTrackerSourceAdapterResolution.Failed(sourceId, failure)
        if (!applicable) {
            return EntryTrackerSourceAdapterResolution.Unavailable(
                sourceId = sourceId,
                reasons = buildSet {
                    if (settingsResult !is EntrySourceSettingsResolution.Available) {
                        add(EntryTrackerSourceAdapterUnavailableReason.SETTINGS)
                    }
                    if (homeResult !is EntrySourceHomeResolution.Available) {
                        add(EntryTrackerSourceAdapterUnavailableReason.HOME)
                    }
                    if (imageSource == null) add(EntryTrackerSourceAdapterUnavailableReason.IMAGE_CLIENT)
                },
            )
        }

        return EntryTrackerSourceAdapterResolution.Available(
            sourceId = sourceId,
            preferences = settingsResult.preferences,
            homeUrl = homeResult.url,
            imageClient = checkNotNull(imageSource).client,
        )
    }
}
