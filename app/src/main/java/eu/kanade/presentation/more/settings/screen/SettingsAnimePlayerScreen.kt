package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.animeplayer.MpvAddonSettingsRow
import mihon.app.di.appGraph
import mihon.entry.interactions.settings.AnimeMarkerSkipMode
import mihon.entry.interactions.settings.AnimePlayerOrientation
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.viewer.settings.asProfilePreference
import mihon.feature.appupdate.mpvaddon.restartKatari
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

object SettingsAnimePlayerScreen : AppEntryViewerSettingsScreenProjection() {

    override val surfaceId: String = AnimePlayerPreferences.PROVIDER_ID

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_anime_player

    @Composable
    override fun getSurfacePreferences(): List<Preference> {
        val context = LocalContext.current
        val animePlayerPreferences = remember { context.appGraph.animePlayerPreferences }
        val mpvAddon = remember { context.appGraph.mpvAddon }
        val mpvAddonInstallation = remember { context.appGraph.mpvAddonInstallation }
        val settingBinder = remember { context.appGraph.viewerSettingBinder }
        val pictureInPicture = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.autoEnterPictureInPictureSetting).asProfilePreference()
        }
        val seekPreview = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.seekPreviewSetting).asProfilePreference()
        }
        val orientation = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.orientationSetting).asProfilePreference()
        }
        val autoPlayNext = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.autoPlayNextSetting).asProfilePreference()
        }
        val markerSkip = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.markerSkipSetting).asProfilePreference()
        }
        val backgroundPlayback = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.backgroundPlaybackSetting).asProfilePreference()
        }
        val preferredQuality = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.preferredQualityHeightSetting).asProfilePreference()
        }
        val defaultSpeed = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.defaultPlaybackSpeedPercentSetting).asProfilePreference()
        }
        val seekStep = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.seekStepSetting).asProfilePreference()
        }

        val playback = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = orientation,
                entries = AnimePlayerOrientation.entries.associateWith {
                    when (it) {
                        AnimePlayerOrientation.FOLLOW_VIDEO -> stringResource(MR.strings.anime_orientation_follow_video)
                        AnimePlayerOrientation.LANDSCAPE -> stringResource(MR.strings.rotation_landscape)
                        AnimePlayerOrientation.PORTRAIT -> stringResource(MR.strings.rotation_portrait)
                        AnimePlayerOrientation.SYSTEM -> stringResource(MR.strings.anime_orientation_system)
                    }
                },
                title = stringResource(MR.strings.pref_anime_orientation),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = preferredQuality,
                entries = AnimePlayerPreferences.PREFERRED_QUALITY_HEIGHT_OPTIONS.associateWith {
                    if (it == 0) stringResource(MR.strings.anime_quality_highest) else "${it}p"
                },
                title = stringResource(MR.strings.pref_anime_preferred_quality),
                subtitle = stringResource(MR.strings.pref_anime_preferred_quality_summary),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = defaultSpeed,
                entries = AnimePlayerPreferences.DEFAULT_PLAYBACK_SPEED_PERCENT_OPTIONS.associateWith {
                    "${(it / 100f).toString().removeSuffix(".0")}x"
                },
                title = stringResource(MR.strings.pref_anime_default_speed),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = autoPlayNext,
                title = stringResource(MR.strings.pref_anime_auto_play_next),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = markerSkip,
                entries = AnimeMarkerSkipMode.entries.associateWith {
                    when (it) {
                        AnimeMarkerSkipMode.BUTTON -> stringResource(MR.strings.pref_anime_marker_skip_button)
                        AnimeMarkerSkipMode.AUTOMATIC -> stringResource(MR.strings.pref_anime_marker_skip_automatic)
                        AnimeMarkerSkipMode.OFF -> stringResource(MR.strings.off)
                    }
                },
                title = stringResource(MR.strings.anime_marker_skip),
                subtitle = stringResource(MR.strings.pref_anime_marker_skip_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = backgroundPlayback,
                title = stringResource(MR.strings.pref_anime_background_playback),
                subtitle = stringResource(MR.strings.pref_anime_background_playback_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = pictureInPicture,
                title = stringResource(MR.strings.pref_anime_auto_enter_picture_in_picture),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = seekPreview,
                title = stringResource(MR.strings.pref_enable_anime_seek_preview),
                subtitle = stringResource(MR.strings.pref_enable_anime_seek_preview_summary),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = seekStep,
                entries = AnimePlayerPreferences.SEEK_STEP_SECONDS_OPTIONS.associateWith {
                    stringResource(MR.strings.seconds_short, it)
                },
                title = stringResource(MR.strings.pref_anime_seek_step),
                subtitle = stringResource(MR.strings.pref_anime_seek_step_summary),
            ),
        )

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_anime_playback),
                preferenceItems = playback,
            ),
            // The add-on is installed once for the device, so its group has no profile chip, which moves the screen's
            // chip from its title onto the playback group.
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.mpv_addon_section),
                preferenceItems = listOf(
                    Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.mpv_addon)) {
                        MpvAddonSettingsRow(
                            addon = mpvAddon,
                            installation = mpvAddonInstallation,
                            onRestart = { restartKatari(context) },
                        )
                    },
                ),
            ),
        )
    }
}
