package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Row
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.i18n.*

/**
 * Collects what a Mihon-family backup carries while its entries stream past. An entry the backup holds more than once is
 * counted once: restoring combines its copies (D034).
 */
internal class MihonConversionReport(private val merges: MihonMerges) {
    private val library = linkedMapOf<EntryKey, String>()
    private val progress = linkedMapOf<EntryKey, String>()
    private val tracked = linkedMapOf<EntryKey, String>()
    private val customTitles = linkedMapOf<EntryKey, String>()
    private val customInfoLeftBehind = linkedMapOf<EntryKey, List<Item>>()
    private val searchMetadata = linkedMapOf<EntryKey, String>()
    private val clientData = linkedMapOf<EntryKey, String>()
    private val entrySources = hashSetOf<Long>()

    fun add(manga: MihonManga) {
        val key = EntryKey(manga.source, manga.url)
        val title = manga.customTitle ?: manga.title
        // A merge's members are its titles in the library, which the merged entry stands for
        if (manga.favorite && !merges.isMember(manga)) library[key] = title
        if (manga.hasProgress) progress[key] = title
        if (manga.tracking.isNotEmpty()) tracked[key] = title
        if (manga.customTitle != null) customTitles[key] = title
        manga.customInfoLeftBehind.takeIf { it.isNotEmpty() }?.let { notes ->
            customInfoLeftBehind[key] = notes.map { Item(title, it) }
        }
        if (manga.flatMetadata != null) searchMetadata[key] = title
        if (manga.hasSuwayomiMeta) clientData[key] = title
        if (!merges.isMergedEntry(manga)) entrySources += manga.source
    }

    fun build(
        backup: MihonBackup,
        settings: ConvertedMihonSettings,
        savedSearches: ConvertedSavedSearches,
    ): ConversionReport {
        val (stores, repos) = backup.extensionSources.partition { it.isStore }
        val sourceNames = backup.sources.associate { it.sourceId to it.name }
        val extensions = backup.sources.filter { it.sourceId in entrySources && it.sourceId != LOCAL_SOURCE_ID }
        return ConversionReport(
            listOfNotNull(
                Row.of(Kind.LIBRARY, carried = library.values.map { Item(it) }),
                Row.of(Kind.CATEGORIES, carried = backup.categories.map { it.reportItem() }),
                Row.of(Kind.PROGRESS_AND_HISTORY, carried = progress.values.map { Item(it) }),
                Row.of(
                    Kind.MERGED_ENTRIES,
                    carried = merges.merges.map { Item(it.title) },
                    leftBehind = merges.merges.flatMap { it.leftBehind() },
                ),
                Row.of(
                    Kind.CUSTOM_INFO,
                    carried = customTitles.values.map { Item(it, MR.strings.backup_conversion_custom_title) },
                    leftBehind = customInfoLeftBehind.values.flatten(),
                ),
                Row.of(
                    Kind.APP_SETTINGS,
                    carried = settings.carried,
                    leftBehind = settings.leftBehind + backup.suwayomiSettingsLeftBehind(),
                ),
                Row.of(
                    Kind.EXTENSION_SETTINGS,
                    carried = backup.sourcePreferences.map { preferences ->
                        val sourceId = preferences.sourceKey.removePrefix(SOURCE_KEY_PREFIX).toLongOrNull()
                        Item(sourceNames[sourceId] ?: preferences.sourceKey)
                    },
                ),
                Row.of(
                    Kind.EXTENSION_STORES,
                    carried = stores.map { Item(it.name) },
                    leftBehind = repos.map { Item(it.url, MR.strings.backup_conversion_old_repo_format) },
                ),
                Row.of(
                    Kind.INSTALLED_EXTENSIONS,
                    leftBehind = extensions.map { Item(it.name, MR.strings.backup_conversion_install_from_store) },
                ),
                Row.of(
                    Kind.TRACKERS,
                    leftBehind = tracked.values.map { Item(it, MR.strings.backup_conversion_no_trackers) },
                ),
                Row.of(
                    Kind.APP_SPECIFIC,
                    carried = savedSearches.carried,
                    leftBehind = searchMetadata.values.map { Item(it, MR.strings.backup_conversion_search_metadata) } +
                        clientData.values.map { Item(it, MR.strings.backup_conversion_client_data) },
                ),
            ),
        )
    }

    private fun MihonBackup.suwayomiSettingsLeftBehind(): List<Item> = listOfNotNull(
        MR.strings.backup_conversion_server_settings
            .takeIf { suwayomiServerSettings != null || suwayomiUserSettings != null },
        MR.strings.backup_conversion_client_settings.takeIf { suwayomiMeta.isNotEmpty() },
    ).map { Item(Name.Part(it), MR.strings.backup_conversion_app_own_setting) }

    private fun MihonMerges.Merge.leftBehind(): List<Item> = buildList {
        repeat(builtInSourceMembers) { add(Item(title, MR.strings.backup_conversion_merge_builtin_source)) }
        if (hasChapterSortMode) add(Item(title, MR.strings.backup_conversion_merge_sort_mode))
        repeat(downloadsOff) { add(Item(title, MR.strings.backup_conversion_merge_download_switch)) }
    }
}

// Mihon names a source's settings after its id (ConfigurableSource.preferenceKey)
private const val SOURCE_KEY_PREFIX = "source_"
