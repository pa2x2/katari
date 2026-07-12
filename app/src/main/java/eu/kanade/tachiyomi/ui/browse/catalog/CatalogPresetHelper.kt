package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.latestFeedPreset
import eu.kanade.domain.source.model.popularFeedPreset
import eu.kanade.domain.source.service.BrowseFeedService
import tachiyomi.domain.source.service.SourceManager

/**
 * Helpers for feed presets in the unified catalog screen.
 */
class CatalogPresetHelper(
    private val sourceId: Long,
    private val sourceManager: SourceManager,
    private val browseFeedService: BrowseFeedService,
) {

    val supportsLatest: Boolean
        get() = sourceManager.getCatalogueSource(sourceId)?.supportsLatest ?: false

    fun feedPresets(): List<SourceFeedPreset> {
        val custom = browseFeedService.stateSnapshot().presets
            .filter { it.sourceId == sourceId }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        return buildList {
            add(popularFeedPreset(sourceId, "Popular"))
            if (supportsLatest) {
                add(latestFeedPreset(sourceId, "Latest"))
            }
            addAll(custom)
        }
    }

    fun customPreset(presetId: String?): SourceFeedPreset? {
        val targetPresetId = presetId ?: return null
        return browseFeedService.stateSnapshot().presets.firstOrNull {
            it.id == targetPresetId && it.sourceId == sourceId
        }
    }

    fun canDeletePreset(presetId: String): Boolean {
        return presetId != BUILTIN_POPULAR_PRESET_ID && presetId != BUILTIN_LATEST_PRESET_ID
    }

    fun hasPresetName(name: String, excludingPresetId: String? = null): Boolean {
        val trimmed = name.trim()
        return browseFeedService.stateSnapshot().presets.any {
            it.sourceId == sourceId &&
                it.id != excludingPresetId && it.name.equals(trimmed, ignoreCase = true)
        }
    }

    fun savePreset(preset: SourceFeedPreset) {
        browseFeedService.savePreset(preset)
    }

    fun removePreset(presetId: String) {
        browseFeedService.removePreset(presetId)
    }
}
