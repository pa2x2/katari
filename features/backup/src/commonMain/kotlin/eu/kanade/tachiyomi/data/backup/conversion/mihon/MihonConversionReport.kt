package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Row
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedSettings
import eu.kanade.tachiyomi.data.backup.conversion.CustomInfoReport
import eu.kanade.tachiyomi.data.backup.conversion.extensionItem
import eu.kanade.tachiyomi.data.backup.conversion.extensionSettingsRow
import eu.kanade.tachiyomi.data.backup.conversion.reportName
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.i18n.*

/**
 * Collects what a Mihon-family backup carries while its entries stream past. An entry the backup holds more than once is
 * counted once: restoring combines its copies (D034).
 */
internal class MihonConversionReport(private val merges: MihonMerges) {
    private val library = linkedMapOf<EntryKey, String>()
    private val progress = linkedMapOf<EntryKey, String>()
    private val historyWithoutChapters = linkedMapOf<EntryKey, String>()
    private val tracked = linkedMapOf<EntryKey, String>()
    private val customInfo = CustomInfoReport<EntryKey>()
    private val searchMetadata = linkedMapOf<EntryKey, String>()
    private val clientData = linkedMapOf<EntryKey, String>()
    private val entrySources = hashSetOf<Long>()

    fun add(manga: MihonManga) {
        val key = EntryKey(manga.source, manga.url)
        val title = manga.customTitle ?: manga.title
        // A merge's members are its titles in the library, which the merged entry stands for
        if (manga.favorite && !merges.isMember(manga.source, manga.url)) library[key] = title
        if (manga.hasProgress) progress[key] = title
        if (manga.hasHistoryWithoutChapters) historyWithoutChapters[key] = title
        if (manga.tracking.isNotEmpty()) tracked[key] = title
        customInfo.add(key, title, manga.customInfo)
        if (manga.flatMetadata != null) searchMetadata[key] = title
        if (manga.hasSuwayomiMeta) clientData[key] = title
        if (!merges.isMergedEntry(manga.source)) entrySources += manga.source
    }

    /** Adds [manga], an entry the converted backup doesn't keep, for its history, none of it on a chapter it holds. */
    fun addNotKept(manga: MihonManga) {
        if (manga.history.isNotEmpty()) {
            historyWithoutChapters[EntryKey(manga.source, manga.url)] = manga.customTitle ?: manga.title
        }
    }

    fun build(
        backup: MihonBackup,
        categories: ConvertedCategories,
        settings: ConvertedSettings,
        savedSearches: ConvertedSavedSearches,
    ): ConversionReport {
        val (stores, repos) = backup.extensionSources.partition { it.isStore }
        val sourceNames = backup.sources.associate { it.sourceId to it.reportName }
        val extensions = backup.sources.filter { it.sourceId in entrySources && it.sourceId != LOCAL_SOURCE_ID }
        return ConversionReport(
            listOfNotNull(
                Row.of(Kind.LIBRARY, carried = library.values.map { Item(it) }),
                Row.of(Kind.CATEGORIES, carried = categories.reportItems),
                Row.of(
                    Kind.PROGRESS_AND_HISTORY,
                    carried = progress.values.map { Item(it) },
                    leftBehind = historyWithoutChapters.values.map {
                        Item(it, MR.strings.backup_conversion_history_without_chapters)
                    },
                ),
                Row.of(
                    Kind.MERGED_ENTRIES,
                    carried = merges.reportItems,
                    leftBehind = merges.leftBehind,
                ),
                customInfo.row(),
                Row.of(
                    Kind.APP_SETTINGS,
                    carried = settings.carried,
                    leftBehind = settings.leftBehind + backup.suwayomiSettingsLeftBehind(),
                ),
                extensionSettingsRow(backup.sourcePreferences, sourceNames),
                Row.of(
                    Kind.EXTENSION_STORES,
                    carried = stores.map { Item(it.name) },
                    leftBehind = repos.map { Item(it.url, MR.strings.backup_conversion_old_repo_format) },
                ),
                Row.of(
                    Kind.INSTALLED_EXTENSIONS,
                    leftBehind = extensions.map { it.extensionItem },
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
}
