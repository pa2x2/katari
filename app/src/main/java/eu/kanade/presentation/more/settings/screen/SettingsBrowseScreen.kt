package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import kotlinx.collections.immutable.toImmutableMap
import mihon.core.common.CustomPreferences
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.entry.interactions.EntryPreviewSize
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.collectAsState as collectFlowAsState

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val customPreferences = remember { Injekt.get<CustomPreferences>() }
        val entryInteractionPreferences = remember { Injekt.get<EntryInteractionPreferences>() }
        val getExtensionStoreCountAsFlow = remember { Injekt.get<GetExtensionStoreCountAsFlow>() }

        val reposCount by getExtensionStoreCountAsFlow().collectFlowAsState(0)
        val browseLongPressAction by customPreferences.browseLongPressAction.collectAsState()
        val mangaPreviewEnabled by entryInteractionPreferences.enableMangaPreview.collectAsState()
        val animePreviewEnabled by entryInteractionPreferences.enableAnimePreview.collectAsState()
        val anyPreviewEnabled = mangaPreviewEnabled || animePreviewEnabled

        LaunchedEffect(anyPreviewEnabled, browseLongPressAction) {
            if (!anyPreviewEnabled && browseLongPressAction == CustomPreferences.BrowseLongPressAction.PREVIEW) {
                customPreferences.browseLongPressAction.set(CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION)
            }
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInLibraryItems,
                        title = stringResource(MR.strings.pref_hide_in_library_items),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.extensionStores),
                        subtitle = pluralStringResource(MR.plurals.num_repos, reposCount.toInt(), reposCount),
                        onClick = {
                            navigator.push(ExtensionStoresScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_nsfw_content),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.showNsfwSource,
                        title = stringResource(MR.strings.pref_show_nsfw_source),
                        subtitle = stringResource(MR.strings.requires_app_restart),
                        onValueChanged = {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(MR.strings.pref_category_nsfw_content),
                            )
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.parental_controls_info)),
                ),
            ),
            getBrowseBehaviorGroup(
                customPreferences = customPreferences,
                anyPreviewEnabled = anyPreviewEnabled,
            ),
            getPreviewGroup(
                entryInteractionPreferences = entryInteractionPreferences,
                mangaPreviewEnabled = mangaPreviewEnabled,
                animePreviewEnabled = animePreviewEnabled,
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_duplicate_detection),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_duplicate_detection),
                        subtitle = stringResource(MR.strings.pref_duplicate_detection_summary),
                        isProfileSpecific = true,
                        onClick = {
                            navigator.push(DuplicateDetectionSettingsScreen)
                        },
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun getBrowseBehaviorGroup(
        customPreferences: CustomPreferences,
        anyPreviewEnabled: Boolean,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_behavior),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = customPreferences.enableFeeds,
                    title = stringResource(MR.strings.pref_enable_feeds),
                    subtitle = stringResource(MR.strings.pref_enable_feeds_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = customPreferences.browseLongPressAction,
                    entries = CustomPreferences.BrowseLongPressAction.entries
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_browse_long_press_action),
                    entryEnabledProvider = {
                        anyPreviewEnabled || it != CustomPreferences.BrowseLongPressAction.PREVIEW
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getPreviewGroup(
        entryInteractionPreferences: EntryInteractionPreferences,
        mangaPreviewEnabled: Boolean,
        animePreviewEnabled: Boolean,
    ): Preference.PreferenceGroup {
        val mangaPreviewPageCount by entryInteractionPreferences.mangaPreviewPageCount.collectAsState()
        val animePreviewPageCount by entryInteractionPreferences.animePreviewPageCount.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_browse_long_press_action_preview),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = entryInteractionPreferences.enableMangaPreview,
                    title = stringResource(MR.strings.pref_enable_manga_preview),
                    subtitle = stringResource(MR.strings.pref_enable_manga_preview_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = entryInteractionPreferences.enableAnimePreview,
                    title = stringResource(MR.strings.pref_enable_anime_preview),
                    subtitle = stringResource(MR.strings.pref_enable_anime_preview_summary),
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(MR.strings.pref_anime_preview_source_support_info),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = mangaPreviewPageCount,
                    preference = entryInteractionPreferences.mangaPreviewPageCount,
                    valueRange = EntryInteractionPreferences.PREVIEW_PAGE_COUNT_RANGE,
                    title = stringResource(MR.strings.pref_manga_preview_page_count),
                    valueString = mangaPreviewPageCount.toString(),
                    enabled = mangaPreviewEnabled,
                    onValueChanged = {
                        entryInteractionPreferences.mangaPreviewPageCount.set(it)
                    },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = animePreviewPageCount,
                    preference = entryInteractionPreferences.animePreviewPageCount,
                    valueRange = EntryInteractionPreferences.PREVIEW_PAGE_COUNT_RANGE,
                    title = stringResource(MR.strings.pref_anime_preview_page_count),
                    valueString = animePreviewPageCount.toString(),
                    enabled = animePreviewEnabled,
                    onValueChanged = {
                        entryInteractionPreferences.animePreviewPageCount.set(it)
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = entryInteractionPreferences.mangaPreviewSize,
                    entries = EntryPreviewSize.entries
                        .associateWith { stringResource(it.previewSizeTitleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_manga_preview_size),
                    enabled = mangaPreviewEnabled,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = entryInteractionPreferences.animePreviewSize,
                    entries = EntryPreviewSize.entries
                        .associateWith { stringResource(it.previewSizeTitleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_anime_preview_size),
                    enabled = animePreviewEnabled,
                ),
            ),
        )
    }

    private val EntryPreviewSize.previewSizeTitleRes
        get() = when (this) {
            EntryPreviewSize.SMALL -> MR.strings.pref_manga_preview_size_small
            EntryPreviewSize.MEDIUM -> MR.strings.pref_manga_preview_size_medium
            EntryPreviewSize.LARGE -> MR.strings.pref_manga_preview_size_large
            EntryPreviewSize.EXTRA_LARGE -> MR.strings.pref_manga_preview_size_extra_large
        }
}
