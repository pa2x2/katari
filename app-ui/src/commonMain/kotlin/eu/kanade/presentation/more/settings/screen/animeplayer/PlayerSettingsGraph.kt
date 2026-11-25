package eu.kanade.presentation.more.settings.screen.animeplayer

import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.DefaultSubtitleLookPage

/** Graph entry points of the anime player settings, beyond the settings its surface provides. */
interface PlayerSettingsGraph {
    val animePlayerDevicePreferences: AnimePlayerDevicePreferences
    val playerEngineAddon: PlayerEngineAddon
    val defaultSubtitleLookPage: DefaultSubtitleLookPage
}
