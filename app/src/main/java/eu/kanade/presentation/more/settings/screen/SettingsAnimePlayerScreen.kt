package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import mihon.app.di.appGraph
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
            settingBinder.bind(animePlayerPreferences.pictureInPictureSetting).asProfilePreference()
        }
        val seekPreview = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.seekPreviewSetting).asProfilePreference()
        }
        val seekStep = remember(animePlayerPreferences, settingBinder) {
            settingBinder.bind(animePlayerPreferences.seekStepSetting).asProfilePreference()
        }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = pictureInPicture,
                title = stringResource(MR.strings.pref_enable_anime_picture_in_picture),
                subtitle = stringResource(MR.strings.pref_enable_anime_picture_in_picture_summary),
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
