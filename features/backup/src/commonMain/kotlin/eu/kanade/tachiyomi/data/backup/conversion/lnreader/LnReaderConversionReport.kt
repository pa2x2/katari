package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Row
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedSettings
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderChapterList
import eu.kanade.tachiyomi.data.backup.conversion.reportName
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import tachiyomi.i18n.*

/** Collects what an LNReader backup carries while its novels stream past. */
internal class LnReaderConversionReport {
    private val library = linkedMapOf<NovelKey, String>()
    private val progress = linkedMapOf<NovelKey, String>()
    private val imported = linkedMapOf<NovelKey, String>()
    private val tracked = linkedMapOf<NovelKey, String>()
    private val unsortable = linkedMapOf<NovelKey, String>()

    /** Adds [novel], whose chapters are listed as [chapterList] says and which [tracked] says a tracker follows. */
    fun add(novel: LnReaderNovel, chapterList: LnReaderChapterList, tracked: Boolean) {
        val key = NovelKey(novel.pluginId, novel.path)
        if (tracked) this.tracked[key] = novel.name
        if (novel.isLocal) {
            if (novel.inLibrary || novel.hasProgress) imported[key] = novel.name
            return
        }
        if (novel.inLibrary) library[key] = novel.name
        if (novel.hasProgress) progress[key] = novel.name
        if (chapterList.unsortable) unsortable[key] = novel.name
    }

    fun build(
        categories: LnReaderCategories,
        sources: List<BackupSource>,
        stores: List<BackupExtensionStore>,
        files: LnReaderFiles,
        settings: ConvertedSettings,
    ): ConversionReport = ConversionReport(
        listOfNotNull(
            Row.of(
                Kind.LIBRARY,
                carried = library.values.map { Item(it) },
                leftBehind = imported.values.map { Item(it, MR.strings.backup_conversion_imported_book) },
            ),
            Row.of(Kind.CATEGORIES, carried = categories.reportItems),
            Row.of(Kind.PROGRESS_AND_HISTORY, carried = progress.values.map { Item(it) }),
            Row.of(Kind.APP_SETTINGS, carried = settings.carried, leftBehind = settings.leftBehind),
            Row.of(
                Kind.EXTENSION_STORES,
                carried = stores.map { Item(it.name, MR.strings.backup_conversion_store_from_addresses) },
            ),
            Row.of(
                Kind.INSTALLED_EXTENSIONS,
                leftBehind = sources.map { Item(it.reportName, INSTALL_FROM_STORE) } +
                    listOfNotNull(
                        part(MR.strings.backup_conversion_extension_files, INSTALL_FROM_STORE, files.pluginCode),
                    ),
            ),
            Row.of(
                Kind.TRACKERS,
                leftBehind = tracked.values.map {
                    Item(it, MR.strings.backup_conversion_no_trackers)
                },
            ),
            Row.of(
                Kind.APP_SPECIFIC,
                leftBehind = unsortable.values.map { Item(it, MR.strings.backup_conversion_lnreader_sort_read_time) } +
                    listOfNotNull(
                        part(
                            MR.strings.backup_conversion_saved_covers,
                            MR.strings.backup_conversion_covers_reload,
                            files.covers,
                        ),
                        part(
                            MR.strings.backup_conversion_downloaded_chapters,
                            MR.strings.backup_conversion_download_again,
                            files.downloads,
                        ),
                    ),
            ),
        ),
    )

    private fun part(label: StringResource, note: StringResource, present: Boolean): Item? =
        Item(Name.Part(label), note).takeIf { present }

    private data class NovelKey(val pluginId: String, val path: String)
}

private val INSTALL_FROM_STORE = MR.strings.backup_conversion_install_from_store

/** Which of the files LNReader can put in a backup this one holds; none of them is carried (D080). */
internal class LnReaderFiles(val covers: Boolean, val pluginCode: Boolean, val downloads: Boolean)
