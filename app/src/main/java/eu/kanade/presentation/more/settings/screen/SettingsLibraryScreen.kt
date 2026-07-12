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
import tachiyomi.domain.library.model.LibraryGroupType
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_HAS_UNCONSUMED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_STARTED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_NEW
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal enum class LibrarySettingsSection {
    Categories,
    Display,
    Group,
    LibraryUpdate,
    Behavior,
}

internal fun visibleLibrarySettingsSections(): List<LibrarySettingsSection> {
    return listOf(
        LibrarySettingsSection.Categories,
        LibrarySettingsSection.Display,
        LibrarySettingsSection.Group,
        LibrarySettingsSection.LibraryUpdate,
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
            if (LibrarySettingsSection.LibraryUpdate in visibleSections) {
                getGlobalUpdateGroup(allCategories, libraryPreferences)
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
    private fun getGlobalUpdateGroup(
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val autoUpdateIntervalPref = libraryPreferences.autoUpdateInterval
        val autoUpdateCategoriesPref = libraryPreferences.updateCategories
        val autoUpdateCategoriesExcludePref = libraryPreferences.updateCategoriesExclude

        val autoUpdateInterval by autoUpdateIntervalPref.collectAsState()

        val included by autoUpdateCategoriesPref.collectAsState()
        val excluded by autoUpdateCategoriesExcludePref.collectAsState()
        var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.categories),
                message = stringResource(MR.strings.pref_library_update_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    autoUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    autoUpdateCategoriesExcludePref.set(newExcluded.map { it.id.toString() }.toSet())
                    showCategoriesDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_library_update),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = autoUpdateIntervalPref,
                    entries = mapOf(
                        0 to stringResource(MR.strings.update_never),
                        12 to stringResource(MR.strings.update_12hour),
                        24 to stringResource(MR.strings.update_24hour),
                        48 to stringResource(MR.strings.update_48hour),
                        72 to stringResource(MR.strings.update_72hour),
                        168 to stringResource(MR.strings.update_weekly),
                    ),
                    title = stringResource(MR.strings.pref_library_update_interval),
                    onValueChanged = {
                        LibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateDeviceRestrictions,
                    entries = mapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                        DEVICE_CHARGING to stringResource(MR.strings.charging),
                    ),
                    title = stringResource(MR.strings.pref_library_update_restriction),
                    subtitle = stringResource(MR.strings.restrictions),
                    enabled = autoUpdateInterval > 0,
                    onValueChanged = {
                        // Post to event looper to allow the preference to be updated.
                        ContextCompat.getMainExecutor(context).execute { LibraryUpdateJob.setupTask(context) }
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allCategories,
                        included = included,
                        excluded = excluded,
                    ),
                    onClick = { showCategoriesDialog = true },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.autoUpdateMetadata,
                    title = stringResource(MR.strings.pref_library_update_refresh_metadata),
                    subtitle = stringResource(MR.strings.pref_library_update_refresh_metadata_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateEntryRestrictions,
                    entries = mapOf(
                        ENTRY_HAS_UNCONSUMED to stringResource(MR.strings.pref_update_only_completely_read),
                        ENTRY_NON_STARTED to stringResource(MR.strings.pref_update_only_started),
                        ENTRY_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                        ENTRY_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                    ),
                    title = stringResource(MR.strings.pref_library_update_smart_update),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.newShowUpdatesCount,
                    title = stringResource(MR.strings.pref_library_update_show_tab_badge),
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
        val groupType by libraryPreferences.groupType.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.action_group),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.groupType,
                    entries = persistentMapOf(
                        LibraryGroupType.Category to stringResource(MR.strings.action_group_category),
                        LibraryGroupType.Type to stringResource(MR.strings.action_group_type),
                        LibraryGroupType.Extension to stringResource(MR.strings.action_group_extension),
                        LibraryGroupType.TypeCategory to stringResource(MR.strings.action_group_type_category),
                        LibraryGroupType.CategoryType to stringResource(MR.strings.action_group_category_type),
                        LibraryGroupType.ExtensionCategory to stringResource(
                            MR.strings.action_group_extension_category,
                        ),
                        LibraryGroupType.CategoryExtension to stringResource(
                            MR.strings.action_group_category_extension,
                        ),
                    ),
                    title = stringResource(MR.strings.action_group),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categoryTabs,
                    title = stringResource(
                        when (groupType) {
                            LibraryGroupType.Category -> MR.strings.action_display_show_tabs
                            LibraryGroupType.Type -> MR.strings.action_display_show_type_tabs
                            LibraryGroupType.Extension -> MR.strings.action_display_show_extension_tabs
                            LibraryGroupType.TypeCategory,
                            LibraryGroupType.CategoryType,
                            LibraryGroupType.ExtensionCategory,
                            LibraryGroupType.CategoryExtension,
                            -> MR.strings.action_display_show_group_tabs
                        },
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categoryNumberOfItems,
                    title = stringResource(MR.strings.action_display_show_number_of_items),
                ),
            ),
        )
    }
}
