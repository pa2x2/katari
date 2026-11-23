package app.katari.sync.ui.arrival.change.named

import app.katari.entry.interactions.media.EntryViewerSettingsFeature
import app.katari.profiles.core.ProfileStore
import app.katari.translation.api.host.TranslationHostActions
import app.katari.tts.api.host.TtsHostActions
import dev.zacsweers.metro.Inject

/**
 * Every synced setting no Settings row stores, each named and told as the place that changes it does
 * ([NamedSetting]). Settings whose classes this module can't reach are read through what their features expose. Built
 * on first use, as the viewers' settings are.
 */
@Inject
class NamedSettings(
    private val profileStore: ProfileStore,
    private val viewerSettings: () -> EntryViewerSettingsFeature,
    private val tts: TtsHostActions,
    private val translation: TranslationHostActions,
) {
    private val all: List<NamedSetting> by lazy {
        appearanceNamedSettings +
            browseNamedSettings +
            libraryNamedSettings +
            statisticsNamedSettings +
            playerNamedSettings +
            readerNamedSettings(viewerSettings(), profileStore) +
            speechNamedSettings(tts, profileStore) +
            translationNamedSettings(translation, profileStore)
    }

    internal fun of(key: String): NamedSetting? = all.firstOrNull { it.matches(key) }
}
