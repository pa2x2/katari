package eu.kanade.tachiyomi.ui.video.player.screen

import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.viewer.settings.ViewerSettingBinder

/** The player settings the player activity and its screen read, each bound to the profile on first use. */
internal class VideoPlayerSettingBindings(
    binder: ViewerSettingBinder,
    preferences: AnimePlayerPreferences,
) {
    val autoEnterPictureInPicture by lazy { binder.bind(preferences.autoEnterPictureInPictureSetting) }
    val seekPreview by lazy { binder.bind(preferences.seekPreviewSetting) }
    val seekStep by lazy { binder.bind(preferences.seekStepSetting) }
    val autoPlayNext by lazy { binder.bind(preferences.autoPlayNextSetting) }
    val orientation by lazy { binder.bind(preferences.orientationSetting) }
    val backgroundPlayback by lazy { binder.bind(preferences.backgroundPlaybackSetting) }
    val defaultPlaybackSpeedPercent by lazy { binder.bind(preferences.defaultPlaybackSpeedPercentSetting) }
}
