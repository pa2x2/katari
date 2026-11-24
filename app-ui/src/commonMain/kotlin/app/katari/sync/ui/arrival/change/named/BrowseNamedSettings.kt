package app.katari.sync.ui.arrival.change.named

import app.katari.core.common.CustomPreferences
import app.katari.coverlayout.labelRes
import app.katari.entry.interactions.settings.EntryInteractionPreferences
import app.katari.platform.presentation.platformPresentation
import app.katari.sync.ui.arrival.change.setChanges
import app.katari.sync.ui.arrival.change.valueChange
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.more.settings.screen.browse.longpress.browseLongPressPrioritySummary
import eu.kanade.presentation.more.settings.screen.previewSizeTitleRes
import kotlinx.serialization.json.Json
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Browse's settings changed where they are used: long-press priorities, extensions' incognito mode and the not rated
 * content warning, catalog display modes, the migration screens' sort and options, and the anime preview.
 */
internal val browseNamedSettings = listOf(
    valueSetting(platformPresentation.browseLongPressPriorityLabel, {
        CustomPreferences(it).browseLongPressActionPriority
    }) { browseLongPressPrioritySummary(it) },
    namedSetting(MR.strings.pref_browse_long_press_action_source_overrides, {
        CustomPreferences(it).browseLongPressActionOverrides
    }) { _, value, _ ->
        val lookups = LocalAppUiGraph.current.changeLookups
        val profileDefault = stringResource(MR.strings.pref_browse_long_press_action_default_profile)
        (value.before.keys + value.after.keys).mapNotNull { source ->
            valueChange(
                lookups.sourceName(source),
                value.before[source]?.let { browseLongPressPrioritySummary(it) } ?: profileDefault,
                value.after[source]?.let { browseLongPressPrioritySummary(it) } ?: profileDefault,
            )
        }
    },
    namedSetting(MR.strings.pref_allowed_content_warnings, { SourcePreferences.hideNotRatedExtensions(it) }) {
            name,
            value,
            _,
        ->
        val notRated = stringResource(MR.strings.ext_content_warning_not_rated)
        setChanges(
            name,
            setOfNotNull(
                notRated.takeUnless {
                    value.before
                },
            ),
            setOfNotNull(notRated.takeUnless { value.after }),
            { it },
        )
    },
    valueSetting(MR.strings.action_display_mode, { SourcePreferences(it, Json).sourceDisplayMode }) {
        stringResource(it.labelRes)
    },
    familySetting(
        prefix = SourcePreferences.SOURCE_DISPLAY_MODE_KEY_FAMILY.value,
        item = String::toLongOrNull,
        name = { source ->
            stringResource(
                MR.strings.sync_setting_source_display_mode,
                LocalAppUiGraph.current.changeLookups.sourceName(source),
            )
        },
        preference = { source -> { SourcePreferences(it, Json).sourceDisplayMode(source) } },
    ) { name, value ->
        listOfNotNull(valueChange(name, stringResource(value.before.labelRes), stringResource(value.after.labelRes)))
    },
    namedSetting(MR.strings.pref_incognito_mode, {
        SourcePreferences(it, Json).incognitoExtensions
    }) { name, value, _ ->
        val lookups = LocalAppUiGraph.current.changeLookups
        setChanges(
            name,
            value.before,
            value.after,
            { lookups.extensionName(it) },
            added = MR.strings.sync_arrived_turned_on_for,
            removed = MR.strings.sync_arrived_turned_off_for,
            readsAlone = true,
        )
    },
    valueSetting(MR.strings.sync_setting_migration_sort, { SourcePreferences(it, Json).migrationSortingMode }) {
        stringResource(
            when (it) {
                SetMigrateSorting.Mode.ALPHABETICAL -> MR.strings.action_sort_alpha
                SetMigrateSorting.Mode.TOTAL -> MR.strings.action_sort_count
            },
        )
    },
    valueSetting(
        MR.strings.sync_setting_migration_sort_direction,
        { SourcePreferences(it, Json).migrationSortingDirection },
    ) {
        stringResource(
            when (it) {
                SetMigrateSorting.Direction.ASCENDING -> MR.strings.action_asc
                SetMigrateSorting.Direction.DESCENDING -> MR.strings.action_desc
            },
        )
    },
    orderSetting(MR.strings.migrationConfigScreen_title, { SourcePreferences(it, Json).migrationSources }) {
        LocalAppUiGraph.current.changeLookups.sourceName(it)
    },
    switchSetting(MR.strings.migrationConfigScreen_removeDownloadsTitle) {
        SourcePreferences.migrationRemoveDownloads(it)
    },
    switchSetting(MR.strings.migrationConfigScreen_deepSearchModeTitle) {
        SourcePreferences(it, Json).migrationDeepSearchMode
    },
    switchSetting(MR.strings.migrationConfigScreen_prioritizeByChaptersTitle) {
        SourcePreferences(it, Json).migrationPrioritizeByChapters
    },
    switchSetting(MR.strings.migrationConfigScreen_hideUnmatchedTitle) {
        SourcePreferences(it, Json).migrationHideUnmatched
    },
    switchSetting(MR.strings.migrationConfigScreen_hideWithoutUpdatesTitle) {
        SourcePreferences(it, Json).migrationHideWithoutUpdates
    },
    switchSetting(MR.strings.pref_enable_anime_preview) { EntryInteractionPreferences(it).enableAnimePreview },
    valueSetting(MR.strings.pref_anime_preview_page_count, { EntryInteractionPreferences(it).animePreviewPageCount }) {
        it.toString()
    },
    valueSetting(MR.strings.pref_anime_preview_size, { EntryInteractionPreferences(it).animePreviewSize }) {
        stringResource(it.previewSizeTitleRes)
    },
)
