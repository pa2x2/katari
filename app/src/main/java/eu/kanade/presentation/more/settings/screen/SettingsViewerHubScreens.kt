package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsInteraction
import mihon.entry.viewer.settings.ViewerSettingsProvider
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsReaderScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_readers

    @Composable
    override fun getPreferences(): List<Preference> = viewerProviderPreferences(ViewerSettingsCategory.READER)
}

object SettingsPlayerScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_players

    @Composable
    override fun getPreferences(): List<Preference> = viewerProviderPreferences(ViewerSettingsCategory.PLAYER)
}

@Composable
private fun viewerProviderPreferences(category: ViewerSettingsCategory): List<Preference> {
    val navigator = LocalNavigator.currentOrThrow
    val interaction = remember { Injekt.get<ViewerSettingsInteraction>() }
    val providers by interaction.providers.collectAsState()

    return providers
        .filter { it.category == category }
        .map { provider ->
            val screen = viewerSettingsScreen(provider.id)
            Preference.PreferenceItem.TextPreference(
                title = screen?.let { stringResource(it.getTitleRes()) } ?: provider.displayName,
                subtitle = provider.unavailableReason ?: provider.description ?: provider.origin,
                enabled = provider.isAvailable && screen != null,
                isProfileSpecific = true,
                onClick = screen?.let { { navigator.push(it) } },
            )
        }
}

internal fun viewerProviderSettingsScreens(
    providers: Collection<ViewerSettingsProvider>,
): List<SearchableSettings> = providers
    .filter(ViewerSettingsProvider::isAvailable)
    .mapNotNull { viewerSettingsScreen(it.id) }
    .distinct()

private fun viewerSettingsScreen(providerId: String): SearchableSettings? = when (providerId) {
    MangaReaderSettingsProvider.PROVIDER_ID -> SettingsMangaReaderScreen
    ReadiumEpubSettingsProvider.PROVIDER_ID -> SettingsReadiumEpubReaderScreen
    AnimePlayerPreferences.PROVIDER_ID -> SettingsAnimePlayerScreen
    else -> null
}
