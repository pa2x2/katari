package eu.kanade.tachiyomi.ui.video.player.screen

import app.katari.entry.interactions.settings.AnimeMarkerSkipMode
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.entry.viewer.settings.ViewerSettingBinding

/**
 * The player settings the player's host and its screen read, each bound to the profile on first use. The skip mode
 * can differ per title, so it is bound for one title at a time.
 */
internal class VideoPlayerSettingBindings(
    private val binder: ViewerSettingBinder,
    private val preferences: AnimePlayerPreferences,
) {
    val autoEnterPictureInPicture by lazy { binder.bind(preferences.autoEnterPictureInPictureSetting) }
    val seekPreview by lazy { binder.bind(preferences.seekPreviewSetting) }
    val seekStep by lazy { binder.bind(preferences.seekStepSetting) }
    val autoPlayNext by lazy { binder.bind(preferences.autoPlayNextSetting) }
    val orientation by lazy { binder.bind(preferences.orientationSetting) }
    val findIntroTimesOnline by lazy { binder.bind(preferences.findIntroTimesOnlineSetting) }
    val introTimesFromAniSkip by lazy { binder.bind(preferences.introTimesFromAniSkipSetting) }
    val introTimesFromTheIntroDb by lazy { binder.bind(preferences.introTimesFromTheIntroDbSetting) }
    val backgroundPlayback by lazy { binder.bind(preferences.backgroundPlaybackSetting) }
    val defaultPlaybackSpeedPercent by lazy { binder.bind(preferences.defaultPlaybackSpeedPercentSetting) }

    // Loaded with the title's override, so an automatic skip never acts on the profile's mode before it arrives.
    suspend fun markerSkip(entryId: Long): ViewerSettingBinding<AnimeMarkerSkipMode> =
        binder.initializeEntry(entryId).bind(preferences.markerSkipSetting)
}
