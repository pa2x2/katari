package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.settings.AnimePlayerPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsPlayerScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_player

    @Composable
    override fun getPreferences(): List<Preference> {
        val animePlayerPreferences = remember { Injekt.get<AnimePlayerPreferences>() }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = animePlayerPreferences.enableAnimePictureInPicture,
                title = stringResource(MR.strings.pref_enable_anime_picture_in_picture),
                subtitle = stringResource(MR.strings.pref_enable_anime_picture_in_picture_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = animePlayerPreferences.enableAnimeSeekPreview,
                title = stringResource(MR.strings.pref_enable_anime_seek_preview),
                subtitle = stringResource(MR.strings.pref_enable_anime_seek_preview_summary),
            ),
        )
    }
}
