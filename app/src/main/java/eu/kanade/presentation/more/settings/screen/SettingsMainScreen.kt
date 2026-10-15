package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import mihon.feature.profiles.core.ProfileManager
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ChromeReaderMode
import mihon.icons.materialsymbols.rounded.AccountCircle
import mihon.icons.materialsymbols.rounded.Code
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.DocumentScanner
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.Palette
import mihon.icons.materialsymbols.rounded.PlayCircle
import mihon.icons.materialsymbols.rounded.RecordVoiceOver
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Security
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.rounded.Sync
import mihon.icons.materialsymbols.rounded.Translate
import mihon.icons.materialsymbols.rounded.Update
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.navigation.rememberPinnedAppBarScrollBehavior
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

object SettingsMainScreen : Screen() {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val items = remember {
            allItems().filter { isSettingsScreenVisible(it.screen) }
        }
        // In two panes the list sits on a paler container than the screen beside it.
        val container = if (twoPane) SurfaceContainer.Lowest else SurfaceContainer.Surface
        val topBarScrollBehavior = rememberPinnedAppBarScrollBehavior()

        Scaffold(
            topBarScrollBehavior = topBarScrollBehavior,
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_search),
                                    icon = MaterialSymbols.Rounded.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            container = container,
            content = { contentPadding ->
                val state = rememberLazyListState()
                val indexSelected = if (twoPane) {
                    val selectedScreen = navigator.items.first()
                    items.indexOfFirst { item ->
                        item.screen::class == selectedScreen::class ||
                            (
                                item.screen == SettingsReaderScreen &&
                                    selectedScreen is AppEntryViewerSettingsScreenProjection
                                )
                    }
                        .also {
                            LaunchedEffect(Unit) {
                                if (it >= 0) {
                                    state.animateScrollToItem(it)
                                }
                                if (it > 0) {
                                    // Lift scroll
                                    topBarScrollBehavior.state.contentOffset =
                                        topBarScrollBehavior.state.heightOffsetLimit
                                }
                            }
                        }
                } else {
                    null
                }

                LazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.hashCode() },
                    ) { index, item ->
                        val row = @Composable {
                            TextPreferenceWidget(
                                title = stringResource(item.titleRes),
                                subtitle = item.formatSubtitle(),
                                icon = item.icon,
                                onPreferenceClick = { navigator.navigate(item.screen, twoPane) },
                            )
                        }
                        if (twoPane) {
                            Surface(
                                modifier = Modifier.padding(horizontal = KatariTheme.spacing.s),
                                container = if (indexSelected ==
                                    index
                                ) {
                                    SurfaceContainer.Selected
                                } else {
                                    SurfaceContainer.None
                                },
                                shape = KatariTheme.shapes.extraLarge,
                                content = row,
                            )
                        } else {
                            row()
                        }
                    }
                }
            },
        )
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource? = null,
        val formatSubtitle: @Composable () -> String? = { subtitleRes?.let { stringResource(it) } },
        val icon: ImageVector,
        val screen: VoyagerScreen,
    )

    private fun allItems() = listOf(
        Item(
            titleRes = MR.strings.pref_category_appearance,
            subtitleRes = MR.strings.pref_appearance_summary,
            icon = MaterialSymbols.Rounded.Palette,
            screen = SettingsAppearanceScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_library,
            subtitleRes = MR.strings.pref_library_summary_unified,
            icon = MaterialSymbols.Rounded.CollectionsBookmark,
            screen = SettingsLibraryScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_library_updates,
            subtitleRes = MR.strings.pref_library_updates_summary,
            icon = MaterialSymbols.Rounded.Update,
            screen = SettingsLibraryUpdatesScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_readers,
            subtitleRes = MR.strings.pref_reader_summary,
            icon = MaterialSymbols.AutoMirroredRounded.ChromeReaderMode,
            screen = SettingsReaderScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_players,
            subtitleRes = MR.strings.pref_player_summary,
            icon = MaterialSymbols.Rounded.PlayCircle,
            screen = SettingsPlayerScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_downloads,
            subtitleRes = MR.strings.pref_downloads_summary,
            icon = MaterialSymbols.Rounded.Download,
            screen = SettingsDownloadScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_tracking,
            subtitleRes = MR.strings.pref_tracking_summary,
            icon = MaterialSymbols.Rounded.Sync,
            screen = SettingsTrackingScreen,
        ),
        Item(
            titleRes = MR.strings.translation_title,
            subtitleRes = MR.strings.translation_settings_summary,
            icon = MaterialSymbols.Rounded.Translate,
            screen = SettingsTranslationScreen,
        ),
        Item(
            titleRes = MR.strings.tts_title,
            subtitleRes = MR.strings.tts_settings_summary,
            icon = MaterialSymbols.Rounded.RecordVoiceOver,
            screen = SettingsTtsScreen,
        ),
        Item(
            titleRes = MR.strings.text_recognition_title,
            subtitleRes = MR.strings.text_recognition_settings_summary,
            icon = MaterialSymbols.Rounded.DocumentScanner,
            screen = SettingsTextRecognitionScreen,
        ),
        Item(
            titleRes = MR.strings.browse,
            subtitleRes = MR.strings.pref_browse_summary,
            icon = MaterialSymbols.Rounded.Explore,
            screen = SettingsBrowseScreen,
        ),
        Item(
            titleRes = MR.strings.label_data_storage,
            subtitleRes = MR.strings.pref_backup_summary,
            icon = MaterialSymbols.Rounded.Storage,
            screen = SettingsDataScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_security,
            subtitleRes = MR.strings.pref_security_summary,
            icon = MaterialSymbols.Rounded.Security,
            screen = SettingsSecurityScreen,
        ),
        Item(
            titleRes = MR.strings.profiles_title,
            subtitleRes = MR.strings.profiles_summary,
            icon = MaterialSymbols.Rounded.AccountCircle,
            screen = SettingsProfilesScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_advanced,
            subtitleRes = MR.strings.pref_advanced_summary,
            icon = MaterialSymbols.Rounded.Code,
            screen = SettingsAdvancedScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_about,
            formatSubtitle = {
                "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
            },
            icon = MaterialSymbols.Rounded.Info,
            screen = AboutScreen,
        ),
    )
}
