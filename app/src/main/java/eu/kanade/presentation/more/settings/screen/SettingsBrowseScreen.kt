package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import kotlinx.collections.immutable.toImmutableMap
import mihon.core.common.CustomPreferences
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.entry.interactions.EntryPreviewFeature
import mihon.entry.interactions.EntryPreviewSettings
import mihon.entry.interactions.EntryPreviewSize
import mihon.entry.interactions.EntryPreviewSourceRequirement
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
        val entryPreviewFeature = remember { Injekt.get<EntryPreviewFeature>() }
        val getExtensionStoreCountAsFlow = remember { Injekt.get<GetExtensionStoreCountAsFlow>() }

        val reposCount by getExtensionStoreCountAsFlow().collectFlowAsState(0)

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
                onOpenLongPressActions = { navigator.push(BrowseLongPressActionsScreen()) },
            ),
            getPreviewGroup(
                settings = entryPreviewFeature.settings,
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
        onOpenLongPressActions: () -> Unit,
    ): Preference.PreferenceGroup {
        val storedPriority by customPreferences.browseLongPressActionPriority.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_behavior),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = customPreferences.enableFeeds,
                    title = stringResource(MR.strings.pref_enable_feeds),
                    subtitle = stringResource(MR.strings.pref_enable_feeds_summary),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_browse_long_press_action),
                    subtitle = browseLongPressPrioritySummary(storedPriority),
                    isProfileSpecific = true,
                    onClick = onOpenLongPressActions,
                ),
            ),
        )
    }

    @Composable
    private fun getPreviewGroup(
        settings: List<EntryPreviewSettings>,
    ): Preference.PreferenceGroup {
        val items = buildList {
            settings.forEach { previewSettings ->
                val enabled by previewSettings.enabled.collectAsState()
                val pageCount by previewSettings.pageCount.collectAsState()
                val typeName = stringResource(previewSettings.type.entryTypePresentation().displayNameLabel)

                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = previewSettings.enabled,
                        title = "$typeName ${stringResource(MR.strings.pref_browse_long_press_action_preview)}",
                    ),
                )
                if (previewSettings.sourceRequirement == EntryPreviewSourceRequirement.PREVIEW_CAPABILITY) {
                    add(
                        Preference.PreferenceItem.InfoPreference(
                            stringResource(MR.strings.pref_anime_preview_source_support_info),
                        ),
                    )
                }
                add(
                    Preference.PreferenceItem.SliderPreference(
                        value = pageCount,
                        preference = previewSettings.pageCount,
                        valueRange = EntryInteractionPreferences.PREVIEW_PAGE_COUNT_RANGE,
                        title = stringResource(MR.strings.pref_manga_preview_page_count),
                        valueString = pageCount.toString(),
                        enabled = enabled,
                        onValueChanged = {
                            previewSettings.pageCount.set(it)
                        },
                    ),
                )
                add(
                    Preference.PreferenceItem.ListPreference(
                        preference = previewSettings.size,
                        entries = EntryPreviewSize.entries
                            .associateWith { stringResource(it.previewSizeTitleRes) }
                            .toImmutableMap(),
                        title = stringResource(MR.strings.pref_manga_preview_size),
                        enabled = enabled,
                    ),
                )
            }
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_browse_long_press_action_preview),
            preferenceItems = items,
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
