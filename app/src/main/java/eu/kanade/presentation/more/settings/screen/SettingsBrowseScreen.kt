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
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate
import kotlinx.collections.immutable.toImmutableMap
import mihon.app.di.appGraph
import mihon.core.common.CustomPreferences
import mihon.domain.extension.model.ContentWarning
import mihon.entry.interactions.media.EntryPreviewSettings
import mihon.entry.interactions.media.EntryPreviewSize
import mihon.entry.interactions.media.EntryPreviewSourceRequirement
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.collectAsState as collectFlowAsState

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { context.appGraph.sourcePreferences }
        val customPreferences = remember { context.appGraph.customPreferences }
        val entryPreviewFeature = remember { context.appGraph.entryPreviewFeature }
        val getExtensionStoreCountAsFlow = remember { context.appGraph.getExtensionStoreCountAsFlow }

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
            getContentWarningsGroup(sourcePreferences),
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
    private fun getContentWarningsGroup(sourcePreferences: SourcePreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_extensions),
            preferenceItems = listOf(
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = sourcePreferences.enabledContentWarnings,
                    entries = mapOf(
                        ContentWarning.SAFE to stringResource(MR.strings.ext_content_warning_safe),
                        ContentWarning.MIXED to stringResource(MR.strings.ext_content_warning_mixed),
                        ContentWarning.NSFW to stringResource(MR.strings.ext_content_warning_nsfw),
                    ),
                    title = stringResource(MR.strings.pref_allowed_content_warnings),
                    subtitleProvider = { value, entries ->
                        remember(value, entries) {
                            entries.filterKeys { it in value }.values.joinToString()
                        }
                            .takeUnless { it.isBlank() }
                            ?: stringResource(MR.strings.none)
                    },
                    onValueChanged = { newValue ->
                        val added = newValue - sourcePreferences.enabledContentWarnings.get()
                        if (added.any { it != ContentWarning.SAFE }) {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(MR.strings.pref_allowed_content_warnings),
                            )
                        } else {
                            true
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = sourcePreferences.applyContentWarningsToInstalled,
                    title = stringResource(MR.strings.pref_apply_content_warnings_to_installed),
                    subtitle = stringResource(MR.strings.pref_apply_content_warnings_to_installed_summary),
                    onValueChanged = { newValue ->
                        if (newValue) {
                            true
                        } else {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(MR.strings.pref_apply_content_warnings_to_installed),
                            )
                        }
                    },
                ),
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.content_warnings_info)),
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
                        preference = previewSettings.pageCount,
                        valueRange = EntryInteractionPreferences.PREVIEW_PAGE_COUNT_RANGE,
                        title = stringResource(MR.strings.pref_manga_preview_page_count),
                        visible = enabled,
                    ),
                )
                add(
                    Preference.PreferenceItem.ListPreference(
                        preference = previewSettings.size,
                        entries = EntryPreviewSize.entries
                            .associateWith { stringResource(it.previewSizeTitleRes) }
                            .toImmutableMap(),
                        title = stringResource(MR.strings.pref_manga_preview_size),
                        visible = enabled,
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
