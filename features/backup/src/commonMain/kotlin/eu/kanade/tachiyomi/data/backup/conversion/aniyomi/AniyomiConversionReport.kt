package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Row
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonManga
import eu.kanade.tachiyomi.data.backup.conversion.mihon.hasProgress
import eu.kanade.tachiyomi.data.backup.conversion.mihon.isStore
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.i18n.*

/**
 * Collects what an Aniyomi-family backup carries while its entries stream past. An entry the backup holds more than
 * once is counted once: restoring combines its copies (D034).
 */
internal class AniyomiConversionReport {
    private val library = linkedMapOf<EntryKey, String>()
    private val progress = linkedMapOf<EntryKey, String>()
    private val tracked = linkedMapOf<EntryKey, String>()
    private val skipIntroLengths = linkedMapOf<EntryKey, String>()
    private val mangaSources = hashSetOf<Long>()
    private val animeSources = hashSetOf<Long>()

    fun add(manga: MihonManga) {
        val key = EntryKey(EntryType.MANGA, manga.source, manga.url)
        if (manga.favorite) library[key] = manga.title
        if (manga.hasProgress) progress[key] = manga.title
        if (manga.tracking.isNotEmpty()) tracked[key] = manga.title
        mangaSources += manga.source
    }

    fun add(anime: AniyomiAnime) {
        val key = EntryKey(EntryType.ANIME, anime.source, anime.url)
        if (anime.favorite) library[key] = anime.title
        if (anime.hasProgress) progress[key] = anime.title
        if (anime.tracking.isNotEmpty()) tracked[key] = anime.title
        if (anime.hasSkipIntroLength) skipIntroLengths[key] = anime.title
        animeSources += anime.source
    }

    fun build(backup: AniyomiBackup, categories: AniyomiCategories): ConversionReport {
        val (stores, repos) = backup.extensionSources.partition { it.isStore }
        val extensions = backup.mangaSources.filter { it.sourceId in mangaSources } +
            backup.animeSources.filter { it.sourceId in animeSources }
        return ConversionReport(
            listOfNotNull(
                Row.of(Kind.LIBRARY, carried = library.values.map { Item(it) }),
                Row.of(Kind.CATEGORIES, carried = categories.reportItems),
                Row.of(Kind.PROGRESS_AND_HISTORY, carried = progress.values.map { Item(it) }),
                Row.of(
                    Kind.EXTENSION_STORES,
                    carried = stores.map { Item(it.name) },
                    leftBehind = repos.map { Item(it.url, MR.strings.backup_conversion_old_repo_format) },
                ),
                Row.of(
                    Kind.INSTALLED_EXTENSIONS,
                    leftBehind = extensions
                        .filter { it.sourceId != LOCAL_SOURCE_ID }
                        .map { Item(it.name, MR.strings.backup_conversion_install_from_store) },
                ),
                Row.of(
                    Kind.TRACKERS,
                    leftBehind = tracked.values.map { Item(it, MR.strings.backup_conversion_no_trackers) },
                ),
                Row.of(
                    Kind.APP_SPECIFIC,
                    leftBehind = skipIntroLengths.values.map {
                        Item(it, MR.strings.backup_conversion_skip_intro_length)
                    },
                ),
            ),
        )
    }

    // Manga and anime sources can share ids, so the type tells their entries apart
    private data class EntryKey(val type: EntryType, val source: Long, val url: String)
}
