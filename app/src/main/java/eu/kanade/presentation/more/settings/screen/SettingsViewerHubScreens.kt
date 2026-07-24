package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.EntryViewerSettingsFeature
import mihon.entry.interactions.EntryViewerSettingsScreenProjection
import mihon.entry.viewer.settings.ViewerSettingsCategory
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal interface AppEntryViewerSettingsScreenProjection : EntryViewerSettingsScreenProjection, SearchableSettings

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
    val feature = remember { Injekt.get<EntryViewerSettingsFeature>() }

    return feature.destinations
        .filter { it.category == category }
        .map { destination ->
            val screen = destination.appScreen
            Preference.PreferenceItem.TextPreference(
                title = stringResource(screen.getTitleRes()),
                subtitle = destination.description ?: destination.origin,
                isProfileSpecific = true,
                onClick = { navigator.push(screen) },
            )
        }
}

internal fun viewerProviderSettingsScreens(
    feature: EntryViewerSettingsFeature,
): List<SearchableSettings> = feature.destinations
    .map { it.appScreen }
    .distinct()

private val mihon.entry.interactions.EntryViewerSettingsDestination.appScreen: AppEntryViewerSettingsScreenProjection
    get() = projection as AppEntryViewerSettingsScreenProjection
