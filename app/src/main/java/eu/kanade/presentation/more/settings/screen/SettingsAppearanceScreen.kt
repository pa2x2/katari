package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.resolveHomeScreenTab
import mihon.core.common.sanitizeHomeScreenTabOrder
import mihon.core.common.sanitizeHomeScreenTabs
import mihon.core.common.toHomeScreenTabPreferenceValue
import mihon.core.common.toHomeScreenTabs
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate

object SettingsAppearanceScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val customPreferences = remember { Injekt.get<CustomPreferences>() }

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
        val context = LocalContext.current

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
                                setAppCompatDelegateThemeMode(it)
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
                    enabled = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
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
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val now = remember { LocalDate.now() }

        val dateFormat by uiPreferences.dateFormat.collectAsState()
        val formattedNow = remember(dateFormat) {
            UiPreferences.dateFormat(dateFormat).format(now)
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_app_language),
                    onClick = { navigator.push(AppLanguageScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode,
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.dateFormat,
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it).format(now)
                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                        },
                    title = stringResource(MR.strings.pref_date_format),
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
        val startupTab by customPreferences.homeScreenStartupTab.collectAsState()
        val homeScreenTabs by customPreferences.homeScreenTabs.collectAsState()
        val storedHomeTabOrder by customPreferences.homeScreenTabOrder.collectAsState()
        val homeScreenTabsTitle = stringResource(MR.strings.pref_home_screen_tabs)
        val startupScreenTitle = stringResource(MR.strings.pref_startup_screen)
        var showHomeTabsDialog by rememberSaveable { mutableStateOf(false) }
        val homeTabEntries = rememberHomeTabEntries()
        val enabledHomeTabs = remember(homeScreenTabs, storedHomeTabOrder) {
            sanitizeHomeScreenTabs(homeScreenTabs.toHomeScreenTabs(), storedHomeTabOrder)
        }
        val resolvedStartupTab = remember(startupTab, enabledHomeTabs, storedHomeTabOrder) {
            resolveHomeScreenTab(
                requestedTab = startupTab,
                enabledTabs = enabledHomeTabs.filterNot { it == HomeScreenTabs.Profiles },
                tabOrder = storedHomeTabOrder,
            )
        }
        val homeTabsSubtitle = remember(enabledHomeTabs, resolvedStartupTab, startupScreenTitle) {
            buildHomeTabsSubtitle(
                enabledTabs = enabledHomeTabs,
                startupTab = resolvedStartupTab,
                homeTabEntries = homeTabEntries,
                startupScreenTitle = startupScreenTitle,
            )
        }

        if (showHomeTabsDialog) {
            val selectedTabs = remember(homeScreenTabs, storedHomeTabOrder) {
                sanitizeHomeScreenTabs(homeScreenTabs.toHomeScreenTabs(), storedHomeTabOrder).toMutableStateList()
            }
            val orderedTabs = remember(storedHomeTabOrder) {
                sanitizeHomeScreenTabOrder(storedHomeTabOrder).toMutableStateList()
            }
            var selectedStartupTab by remember(resolvedStartupTab) {
                mutableStateOf(resolvedStartupTab)
            }
            val listState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(listState, PaddingValues()) { from, to ->
                val fromIndex = orderedTabs.indexOfFirst { it.name == from.key }
                val toIndex = orderedTabs.indexOfFirst { it.name == to.key }
                if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
                orderedTabs.add(toIndex, orderedTabs.removeAt(fromIndex))
            }
            val dialogEnabledTabs = sanitizeHomeScreenTabs(selectedTabs.toSet(), orderedTabs)
                .filterNot { it == HomeScreenTabs.Profiles }

            LaunchedEffect(dialogEnabledTabs, orderedTabs.toList()) {
                val resolvedDialogStartupTab = resolveHomeScreenTab(
                    requestedTab = selectedStartupTab,
                    enabledTabs = dialogEnabledTabs,
                    tabOrder = orderedTabs,
                )
                if (resolvedDialogStartupTab != selectedStartupTab) {
                    selectedStartupTab = resolvedDialogStartupTab
                }
            }

            AlertDialog(
                onDismissRequest = { showHomeTabsDialog = false },
                title = { Text(text = homeScreenTabsTitle) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                    ) {
                        Box {
                            ScrollbarLazyColumn(
                                modifier = Modifier.heightIn(max = 280.dp),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                            ) {
                                items(
                                    items = orderedTabs,
                                    key = { it.name },
                                ) { tab ->
                                    val isSelected = tab in selectedTabs
                                    val isLocked = tab == HomeScreenTabs.More
                                    ReorderableItem(reorderableState, tab.name, enabled = orderedTabs.size > 1) {
                                        HomeTabItem(
                                            label = homeTabEntries.getValue(tab),
                                            checked = isSelected,
                                            visibilityLocked = isLocked,
                                            dragEnabled = orderedTabs.size > 1,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (tab !in selectedTabs) {
                                                        selectedTabs.add(tab)
                                                    }
                                                } else {
                                                    selectedTabs.remove(tab)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            if (listState.canScrollBackward) {
                                HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                            }
                            if (listState.canScrollForward) {
                                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
                            }
                        }
                        Column {
                            Text(
                                text = startupScreenTitle,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                            )
                            dialogEnabledTabs.forEach { tab ->
                                RadioItem(
                                    label = homeTabEntries.getValue(tab),
                                    selected = selectedStartupTab == tab,
                                    onClick = { selectedStartupTab = tab },
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val updatedTabOrder = sanitizeHomeScreenTabOrder(orderedTabs)
                            val newTabs = sanitizeHomeScreenTabs(selectedTabs.toSet(), updatedTabOrder)
                            customPreferences.homeScreenTabOrder.set(updatedTabOrder)
                            customPreferences.homeScreenTabs.set(newTabs.toHomeScreenTabPreferenceValue())
                            val resolvedStartupTab = resolveHomeScreenTab(
                                requestedTab = selectedStartupTab,
                                enabledTabs = newTabs.filterNot { it == HomeScreenTabs.Profiles },
                                tabOrder = updatedTabOrder,
                            )
                            if (resolvedStartupTab != startupTab) {
                                customPreferences.homeScreenStartupTab.set(resolvedStartupTab)
                            }
                            showHomeTabsDialog = false
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHomeTabsDialog = false }) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_home_screen),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = homeScreenTabsTitle,
                    subtitle = homeTabsSubtitle,
                    isProfileSpecific = true,
                    onClick = { showHomeTabsDialog = true },
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
        )
    }

    @Composable
    private fun ReorderableCollectionItemScope.HomeTabItem(
        label: String,
        checked: Boolean,
        visibilityLocked: Boolean,
        dragEnabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            LabeledCheckbox(
                label = label,
                checked = checked,
                enabled = !visibilityLocked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(if (dragEnabled) Modifier.draggableHandle() else Modifier)
                    .padding(MaterialTheme.padding.small),
            )
        }
    }

    private fun buildHomeTabsSubtitle(
        enabledTabs: Collection<HomeScreenTabs>,
        startupTab: HomeScreenTabs,
        homeTabEntries: ImmutableMap<HomeScreenTabs, String>,
        startupScreenTitle: String,
    ): String {
        return buildString {
            append(enabledTabs.joinToString { homeTabEntries.getValue(it) })
            append(" | ")
            append(startupScreenTitle)
            append(": ")
            append(homeTabEntries.getValue(startupTab))
        }
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
