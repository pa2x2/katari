package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import app.katari.platform.presentation.platformPresentation
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.library.grouping.LibraryGroupingSheet
import eu.kanade.presentation.library.grouping.libraryGroupingSummary
import eu.kanade.presentation.library.grouping.showLibraryGroupingTabsLabel
import eu.kanade.presentation.library.settings.labelRes
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_NEW
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

class SettingsLibraryScreen : SearchableSettings() {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalAppUiGraph.current
        val getCategories = graph.getCategories
        val libraryPreferences = graph.libraryPreferences
        val globalLibraryPreferences = graph.globalLibraryPreferences
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())
        val navigator = LocalNavigator.currentOrThrow

        return listOf(
            getCategoriesGroup(navigator, allCategories, libraryPreferences),
            getDisplayGroup(libraryPreferences),
            getCoverBadgesGroup(libraryPreferences),
            getCoverActionsGroup(libraryPreferences),
            getGroupGroup(libraryPreferences),
            getChapterListsGroup(libraryPreferences, globalLibraryPreferences),
        )
    }

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val resetCategoryFlags = LocalAppUiGraph.current.resetCategoryFlags
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
                                resetCategoryFlags.await()
                            }
                        }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getChapterListsGroup(
        libraryPreferences: LibraryPreferences,
        globalLibraryPreferences: GlobalLibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_chapter_lists),
            preferenceItems = listOf(
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
                    title = stringResource(platformPresentation.chapterSwipeToEndLabel),
                ),
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
                    title = stringResource(platformPresentation.chapterSwipeToStartLabel),
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
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.pinnedDisplayStyle,
                    entries = LibraryPinnedDisplayStyle.entries.associateWith { stringResource(it.labelRes) },
                    title = stringResource(MR.strings.pref_library_pinned_display_style),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.coverSize,
                    entries = CoverSize.entries.associateWith { stringResource(it.labelRes) },
                    title = stringResource(MR.strings.pref_library_cover_size),
                    visible = displayMode != LibraryDisplayMode.List &&
                        displayMode != LibraryDisplayMode.ComfortableList,
                ),
            ),
        )
    }

    /** The same badges, in the same order, as the library's Display menu. */
    @Composable
    private fun getCoverBadgesGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup = Preference.PreferenceGroup(
        title = stringResource(MR.strings.overlay_header),
        preferenceItems = persistentListOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.downloadBadge,
                title = stringResource(MR.strings.action_display_download_badge),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.unreadBadge,
                title = stringResource(MR.strings.action_display_unseen_badge),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.localBadge,
                title = stringResource(MR.strings.action_display_local_badge),
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
                preference = libraryPreferences.mergedBadge,
                title = stringResource(MR.strings.action_display_merged_badge),
            ),
        ),
    )

    @Composable
    private fun getCoverActionsGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup = Preference.PreferenceGroup(
        title = stringResource(MR.strings.library_cover_actions_header),
        preferenceItems = persistentListOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.showContinueReadingButton,
                title = stringResource(MR.strings.action_display_show_continue_button),
            ),
        ),
    )

    @Composable
    private fun getGroupGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val grouping by libraryPreferences.grouping.collectAsState()
        var showGroupingSheet by rememberSaveable { mutableStateOf(false) }

        if (showGroupingSheet) {
            LibraryGroupingSheet(
                grouping = grouping,
                onGroupingChange = libraryPreferences.grouping::set,
                onDismissRequest = { showGroupingSheet = false },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.action_group),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.library_grouping_hierarchy),
                    subtitle = libraryGroupingSummary(grouping),
                    storedPreference = libraryPreferences.grouping,
                    onClick = { showGroupingSheet = true },
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
