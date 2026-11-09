package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import app.katari.core.common.CustomPreferences
import app.katari.core.common.HomeScreenTabs
import app.katari.core.common.homeScreenContentTabOrder
import app.katari.core.common.navigation.resolveHomeNavigationConfiguration
import app.katari.core.common.resolveHomeScreenTab
import app.katari.core.common.toHomeScreenTabs
import app.katari.core.platform.currentPlatformContext
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.TimeFormat
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.presentation.more.settings.screen.navigation.HomeNavigationEditorScreen
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import eu.kanade.presentation.util.rememberIs24HourSystem
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.time.Clock

class SettingsAppearanceScreen : SearchableSettings() {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalAppUiGraph.current
        val uiPreferences = graph.uiPreferences
        val customPreferences = graph.customPreferences

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getDisplayGroup(uiPreferences = uiPreferences),
            getHomeScreenGroup(customPreferences = customPreferences),
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = currentPlatformContext()
        val themeHost = LocalAppUiGraph.current.appThemeHost

        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                    isProfileSpecific = true,
                ) {
                    Column {
                        AppThemeModePreferenceWidget(
                            value = themeMode,
                            onItemClick = {
                                themeModePref.set(it)
                                themeHost.applyThemeMode(it)
                            },
                        )

                        AppThemePreferenceWidget(
                            value = appTheme,
                            amoled = amoled,
                            onItemClick = { appThemePref.set(it) },
                        )
                    }
                },
                Preference.PreferenceItem.SwitchPreference(
                    preference = amoledPref,
                    title = stringResource(MR.strings.pref_dark_theme_pure_black),
                    visible = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        themeHost.rebuildForTheme(context)
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val navigator = LocalNavigator.currentOrThrow
        val interfaceSizeSetting = LocalAppUiGraph.current.interfaceSizeSetting

        val locale = LocalLocale.current.platformLocale
        val is24HourSystem = rememberIs24HourSystem()

        val now = remember {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        }

        val dateFormat by uiPreferences.dateFormat.collectAsState()
        val formattedNow = remember(dateFormat, locale) {
            UiPreferences.dateFormat(dateFormat, locale).format(now)
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOfNotNull(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_app_language),
                    onClick = { navigator.push(AppLanguageScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode,
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                ),
                interfaceSizeSetting.preferenceItem(),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.dateFormat,
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it, locale).format(now)
                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                        },
                    title = stringResource(MR.strings.pref_date_format),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.timeFormat,
                    entries = TimeFormat.entries
                        .associateWith {
                            val formattedTime = UiPreferences.timeFormat(it, locale, is24HourSystem).format(now)
                            "${stringResource(it.titleRes)} ($formattedTime)"
                        },
                    title = stringResource(MR.strings.pref_time_format),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.relativeTime,
                    title = stringResource(MR.strings.pref_relative_format),
                    subtitle = stringResource(
                        MR.strings.pref_relative_format_summary,
                        stringResource(MR.strings.relative_time_today),
                        formattedNow,
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.imagesInDescription,
                    title = stringResource(MR.strings.pref_display_images_description),
                ),
            ),
        )
    }

    @Composable
    private fun getHomeScreenGroup(
        customPreferences: CustomPreferences,
    ): Preference.PreferenceGroup {
        val navigator = LocalNavigator.currentOrThrow
        val startupTab by customPreferences.homeScreenStartupTab.collectAsState()
        val homeScreenTabs by customPreferences.homeScreenTabs.collectAsState()
        val storedHomeTabOrder by customPreferences.homeScreenTabOrder.collectAsState()
        val primaryTabs by customPreferences.homeScreenPrimaryTabs.collectAsState()
        val homeScreenTabsTitle = stringResource(MR.strings.pref_home_screen_tabs)
        val startupScreenTitle = stringResource(MR.strings.pref_startup_screen)
        val homeTabEntries = rememberHomeTabEntries()
        val configuration = remember(homeScreenTabs, storedHomeTabOrder, primaryTabs) {
            resolveHomeNavigationConfiguration(
                enabledTabs = homeScreenTabs.toHomeScreenTabs(),
                tabOrder = storedHomeTabOrder,
                primaryTabs = primaryTabs,
            )
        }
        val resolvedStartupTab = remember(startupTab, configuration) {
            resolveHomeScreenTab(
                requestedTab = startupTab,
                enabledTabs = configuration.enabledTabs.filter { it in homeScreenContentTabOrder },
                tabOrder = configuration.tabOrder,
            )
        }
        val homeTabsSubtitle = remember(configuration, resolvedStartupTab, startupScreenTitle, homeTabEntries) {
            buildString {
                append(configuration.primaryTabs.joinToString { homeTabEntries.getValue(it) })
                if (configuration.overflowTabs.isNotEmpty()) {
                    append(" | ")
                    append(configuration.overflowTabs.joinToString { homeTabEntries.getValue(it) })
                }
                append(" | ")
                append(startupScreenTitle)
                append(": ")
                append(homeTabEntries.getValue(resolvedStartupTab))
            }
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_home_screen),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = homeScreenTabsTitle,
                    subtitle = homeTabsSubtitle,
                    isProfileSpecific = true,
                    onClick = { navigator.push(HomeNavigationEditorScreen()) },
                ),
            ),
        )
    }

    @Composable
    private fun rememberHomeTabEntries(): ImmutableMap<HomeScreenTabs, String> {
        return persistentMapOf(
            HomeScreenTabs.Library to stringResource(MR.strings.label_library),
            HomeScreenTabs.Updates to stringResource(MR.strings.label_recent_updates),
            HomeScreenTabs.History to stringResource(MR.strings.history),
            HomeScreenTabs.Browse to stringResource(MR.strings.browse),
            HomeScreenTabs.More to stringResource(MR.strings.label_more),
            HomeScreenTabs.Profiles to stringResource(MR.strings.profiles_switch_summary),
            HomeScreenTabs.Translator to stringResource(MR.strings.translator_title),
            HomeScreenTabs.Statistics to stringResource(MR.strings.label_stats),
        )
    }
}

private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)
