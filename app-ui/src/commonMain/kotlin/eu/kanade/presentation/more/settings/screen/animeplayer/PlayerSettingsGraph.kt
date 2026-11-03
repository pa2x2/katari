package eu.kanade.presentation.more.settings.screen.animeplayer

import mihon.entry.interactions.settings.AnimePlayerDevicePreferences

/** Graph entry points of the anime player settings, beyond the settings its surface provides. */
interface PlayerSettingsGraph {
    val animePlayerDevicePreferences: AnimePlayerDevicePreferences
    val playerEngineAddon: PlayerEngineAddon
}
