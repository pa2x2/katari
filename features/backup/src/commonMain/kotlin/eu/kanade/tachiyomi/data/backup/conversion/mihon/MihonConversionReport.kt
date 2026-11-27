package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.i18n.*

/**
 * Collects what a Mihon-family backup carries while its entries stream past. An entry the backup holds more than once is
 * counted once: restoring combines its copies (D034).
 */
internal class MihonConversionReport {
    private val library = linkedMapOf<Pair<Long, String>, String>()
    private val progress = linkedMapOf<Pair<Long, String>, String>()
    private val tracked = linkedMapOf<Pair<Long, String>, String>()
    private val entrySources = hashSetOf<Long>()

    fun add(manga: MihonManga) {
        val key = manga.source to manga.url
        if (manga.favorite) library[key] = manga.title
        if (manga.hasProgress) progress[key] = manga.title
        if (manga.tracking.isNotEmpty()) tracked[key] = manga.title
        entrySources += manga.source
    }

    fun build(backup: MihonBackup): ConversionReport {
        val (stores, repos) = backup.extensionSources.partition { it.isStore }
        val extensions = backup.sources.filter { it.sourceId in entrySources && it.sourceId != LOCAL_SOURCE_ID }
        return ConversionReport(
            listOfNotNull(
                row(Kind.LIBRARY, carried = library.values.map(::Item)),
                row(Kind.CATEGORIES, carried = backup.categories.map { Item(it.name) }),
                row(Kind.PROGRESS_AND_HISTORY, carried = progress.values.map(::Item)),
                row(
                    Kind.EXTENSION_STORES,
                    carried = stores.map { Item(it.name) },
                    leftBehind = repos.map { Item(it.url, MR.strings.backup_conversion_old_repo_format) },
                ),
                row(
                    Kind.INSTALLED_EXTENSIONS,
                    leftBehind = extensions.map { Item(it.name, MR.strings.backup_conversion_install_from_store) },
                ),
                row(
                    Kind.TRACKERS,
                    leftBehind = tracked.values.map { Item(it, MR.strings.backup_conversion_no_trackers) },
                ),
            ),
        )
    }

    private fun row(kind: Kind, carried: List<Item> = emptyList(), leftBehind: List<Item> = emptyList()) =
        ConversionReport.Row(kind, carried, leftBehind).takeIf { carried.isNotEmpty() || leftBehind.isNotEmpty() }
}
