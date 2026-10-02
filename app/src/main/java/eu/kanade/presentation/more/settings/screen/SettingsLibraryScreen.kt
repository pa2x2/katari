package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.library.grouping.LibraryGroupingDialog
import eu.kanade.presentation.library.grouping.libraryGroupingSummary
import eu.kanade.presentation.library.grouping.showLibraryGroupingTabsLabel
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_NEW
import tachiyomi.domain.library.service.LibraryPreferences.Companion.SKIP_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.SKIP_NOT_STARTED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.SKIP_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.SKIP_UNSEEN
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal enum class LibrarySettingsSection {
    Categories,
    Display,
    Group,
    Behavior,
}

internal fun visibleLibrarySettingsSections(): List<LibrarySettingsSection> {
    return listOf(
        LibrarySettingsSection.Categories,
        LibrarySettingsSection.Display,
        LibrarySettingsSection.Group,
        LibrarySettingsSection.Behavior,
    )
}

object SettingsLibraryScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val globalLibraryPreferences = remember { Injekt.get<GlobalLibraryPreferences>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())
        val navigator = LocalNavigator.currentOrThrow
        val visibleSections = remember {
            visibleLibrarySettingsSections()
        }

        return listOfNotNull(
            if (LibrarySettingsSection.Categories in visibleSections) {
                getCategoriesGroup(navigator, allCategories, libraryPreferences)
            } else {
                null
            },
            if (LibrarySettingsSection.Display in visibleSections) {
                getDisplayGroup(libraryPreferences)
            } else {
                null
            },
            if (LibrarySettingsSection.Group in visibleSections) {
                getGroupGroup(libraryPreferences)
            } else {
                null
            },
            if (LibrarySettingsSection.Behavior in visibleSections) {
                getBehaviorGroup(libraryPreferences, globalLibraryPreferences)
            } else {
                null
            },
        )
    }

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val userCategoriesCount = allCategories.filterNot(Category::isSystemCategory).size

        // For default category
        val ids = listOf(libraryPreferences.defaultCategory.defaultValue()) +
            allCategories.fastMap { it.id.toInt() }
        val labels = listOf(stringResource(MR.strings.default_category_summary)) +
            allCategories.fastMap { it.visualName }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.categories),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.action_edit_categories),
                    subtitle = pluralStringResource(
                        MR.plurals.num_categories,
                        count = userCategoriesCount,
                        userCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoryScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.defaultCategory,
                    entries = ids.zip(labels).toMap(),
                    title = stringResource(MR.strings.default_category),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categorizedDisplaySettings,
                    title = stringResource(MR.strings.categorized_display_settings),
                    onValueChanged = {
                        if (!it) {
                            scope.launch {
                                Injekt.get<ResetCategoryFlags>().await()
                            }
                        }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getBehaviorGroup(
        libraryPreferences: LibraryPreferences,
        globalLibraryPreferences: GlobalLibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_behavior),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeToStartAction,
                    entries = mapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_seen),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(MR.strings.pref_chapter_swipe_start),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeToEndAction,
                    entries = mapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_seen),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(MR.strings.pref_chapter_swipe_end),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = globalLibraryPreferences.markDuplicateReadChapterAsRead,
                    entries = persistentMapOf(
                        MARK_DUPLICATE_CHAPTER_READ_EXISTING to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_existing),
                        MARK_DUPLICATE_CHAPTER_READ_NEW to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_new),
                    ),
                    title = stringResource(MR.strings.pref_mark_duplicate_read_chapter_read),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.hideMissingChapters,
                    title = stringResource(MR.strings.pref_hide_missing_chapter_indicators),
                ),
            ),
        )
    }

    @Composable
    private fun getDisplayGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val displayMode by libraryPreferences.displayMode.collectAsState()
        val portraitColumns by libraryPreferences.portraitColumns.collectAsState()
        val landscapeColumns by libraryPreferences.landscapeColumns.collectAsState()
        val unreadBadgeTitle = MR.strings.action_display_unseen_badge
        val continueButtonTitle = MR.strings.action_display_show_continue_button

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.action_display),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.displayMode,
                    entries = persistentMapOf(
                        LibraryDisplayMode.CompactGrid to stringResource(MR.strings.action_display_grid),
                        LibraryDisplayMode.ComfortableGrid to stringResource(
                            MR.strings.action_display_comfortable_grid,
                        ),
                        LibraryDisplayMode.ComfortableList to stringResource(
                            MR.strings.action_display_comfortable_list,
                        ),
                        LibraryDisplayMode.CoverOnlyGrid to stringResource(MR.strings.action_display_cover_only_grid),
                        LibraryDisplayMode.List to stringResource(MR.strings.action_display_list),
                    ),
                    title = stringResource(MR.strings.action_display_mode),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = portraitColumns,
                    preference = libraryPreferences.portraitColumns,
                    valueRange = 0..10,
                    title = stringResource(MR.strings.portrait),
                    subtitle = stringResource(MR.strings.pref_library_columns),
                    valueString = if (portraitColumns > 0) {
                        portraitColumns.toString()
                    } else {
                        stringResource(MR.strings.label_auto)
                    },
                    enabled = displayMode != LibraryDisplayMode.List &&
                        displayMode != LibraryDisplayMode.ComfortableList,
                    onValueChanged = { libraryPreferences.portraitColumns.set(it) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = landscapeColumns,
                    preference = libraryPreferences.landscapeColumns,
                    valueRange = 0..10,
                    title = stringResource(MR.strings.landscape),
                    subtitle = stringResource(MR.strings.pref_library_columns),
                    valueString = if (landscapeColumns > 0) {
                        landscapeColumns.toString()
                    } else {
                        stringResource(MR.strings.label_auto)
                    },
                    enabled = displayMode != LibraryDisplayMode.List &&
                        displayMode != LibraryDisplayMode.ComfortableList,
                    onValueChanged = { libraryPreferences.landscapeColumns.set(it) },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.unreadBadge,
                    title = stringResource(unreadBadgeTitle),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.languageBadge,
                    title = stringResource(MR.strings.action_display_language_badge),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.entryTypeBadge,
                    title = stringResource(MR.strings.action_display_entry_type_badge),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.showContinueReadingButton,
                    title = stringResource(continueButtonTitle),
                ),
            ),
        )
    }

    @Composable
    private fun getGroupGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val grouping by libraryPreferences.grouping.collectAsState()
        var showGroupingDialog by rememberSaveable { mutableStateOf(false) }

        if (showGroupingDialog) {
            LibraryGroupingDialog(
                initialGrouping = grouping,
                onDismissRequest = { showGroupingDialog = false },
                onApply = {
                    libraryPreferences.grouping.set(it)
                    showGroupingDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.action_group),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.library_grouping_hierarchy),
                    subtitle = libraryGroupingSummary(grouping),
                    isProfileSpecific = true,
                    onClick = { showGroupingDialog = true },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categoryTabs,
                    title = showLibraryGroupingTabsLabel(grouping),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categoryNumberOfItems,
                    title = stringResource(MR.strings.action_display_show_number_of_items),
                ),
            ),
        )
    }
}
