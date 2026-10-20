package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import mihon.app.di.appGraph
import mihon.entry.interactions.settings.AnimePlayerOrientation
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.viewer.settings.asProfilePreference
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

        return listOf(
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
    }
}
