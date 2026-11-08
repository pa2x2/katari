package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * Page text translation settings owned by the manga reader surface.
 *
 * @property translationOverlay draws translations over recognized text instead of translating tapped text only.
 * @property processAheadOnlyOnUnmeteredNetwork limits processing of preloaded pages to unmetered networks.
 * @property processAheadOnlyWhileCharging limits processing of preloaded pages to while the device charges.
 */
class MangaReaderPageTextDefinitions(
    val translationOverlay: ViewerSettingDefinition<Boolean>,
    val processAheadOnlyOnUnmeteredNetwork: ViewerSettingDefinition<Boolean>,
    val processAheadOnlyWhileCharging: ViewerSettingDefinition<Boolean>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        translationOverlay,
        processAheadOnlyOnUnmeteredNetwork,
        processAheadOnlyWhileCharging,
    )
}
