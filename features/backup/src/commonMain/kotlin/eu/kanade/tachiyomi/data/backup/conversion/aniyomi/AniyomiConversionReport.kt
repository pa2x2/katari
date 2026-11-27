package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Row
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedSettings
import eu.kanade.tachiyomi.data.backup.conversion.CustomInfoReport
import eu.kanade.tachiyomi.data.backup.conversion.mihon.ConvertedCategories
import eu.kanade.tachiyomi.data.backup.conversion.mihon.ConvertedSavedSearches
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonManga
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonMerges
import eu.kanade.tachiyomi.data.backup.conversion.mihon.hasProgress
import eu.kanade.tachiyomi.data.backup.conversion.mihon.isStore
import eu.kanade.tachiyomi.data.backup.conversion.reportName
import eu.kanade.tachiyomi.data.backup.sourcekeys.backupSourceKey
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.i18n.*

/**
 * Collects what an Aniyomi-family backup carries while its entries stream past. An entry the backup holds more than
 * once is counted once: restoring combines its copies (D034).
 */
internal class AniyomiConversionReport(private val merges: MihonMerges) {
    private val library = linkedMapOf<EntryKey, String>()
    private val progress = linkedMapOf<EntryKey, String>()
    private val tracked = linkedMapOf<EntryKey, String>()
    private val skipIntroLengths = linkedMapOf<EntryKey, String>()
    private val audioLanguages = linkedMapOf<EntryKey, Item>()
    private val customInfo = CustomInfoReport<EntryKey>()
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
        val title = anime.customInfo.displayName ?: anime.title
        // A merge's members are its titles in the library, which the merged entry stands for
        if (anime.favorite && !merges.isMember(anime.source, anime.url)) library[key] = title
        if (anime.hasProgress) progress[key] = title
        if (anime.tracking.isNotEmpty()) tracked[key] = title
        if (anime.hasSkipIntroLength) skipIntroLengths[key] = title
        customInfo.add(key, title, anime.customInfo)
        anime.audioLanguageItem(title)?.let { audioLanguages[key] = it }
        if (!merges.isMergedEntry(anime.source)) animeSources += anime.source
    }

    fun build(
        backup: AniyomiBackup,
        categories: ConvertedCategories,
        seasons: AniyomiSeasons,
        settings: ConvertedSettings,
        savedSearches: ConvertedSavedSearches,
    ): ConversionReport {
        val (stores, repos) = backup.extensionSources.partition { it.isStore }
        val extensions = backup.mangaSources.filter { it.sourceId in mangaSources } +
            backup.animeSources.filter { it.sourceId in animeSources }
        // Where both kinds have an id, the anime source's name is used
        val sourceNames = (backup.mangaSources + backup.animeSources).associate { it.sourceId to it.reportName }
        return ConversionReport(
            listOfNotNull(
                Row.of(Kind.LIBRARY, carried = library.values.map { Item(it) }),
                Row.of(Kind.CATEGORIES, carried = categories.reportItems),
                Row.of(Kind.PROGRESS_AND_HISTORY, carried = progress.values.map { Item(it) }),
                Row.of(
                    Kind.MERGED_ENTRIES,
                    carried = seasons.parentTitles.map { Item(it, MR.strings.backup_conversion_seasons) } +
                        merges.reportItems,
                    leftBehind = merges.leftBehind,
                ),
                customInfo.row(),
                Row.of(Kind.APP_SETTINGS, carried = settings.carried, leftBehind = settings.leftBehind),
                Row.of(
                    Kind.EXTENSION_SETTINGS,
                    carried = backup.sourcePreferences.map { preferences ->
                        Item(preferences.backupSourceKey?.let(sourceNames::get) ?: preferences.sourceKey)
                    },
                ),
                Row.of(
                    Kind.EXTENSION_STORES,
                    carried = stores.map { Item(it.name) },
                    leftBehind = repos.map { Item(it.url, MR.strings.backup_conversion_old_repo_format) },
                ),
                Row.of(
                    Kind.INSTALLED_EXTENSIONS,
                    leftBehind = extensions
                        .filter { it.sourceId != LOCAL_SOURCE_ID }
                        .map { Item(it.reportName, MR.strings.backup_conversion_install_from_store) },
                ),
                Row.of(
                    Kind.TRACKERS,
                    leftBehind = tracked.values.map { Item(it, MR.strings.backup_conversion_no_trackers) },
                ),
                Row.of(
                    Kind.APP_SPECIFIC,
                    carried = savedSearches.carried,
                    leftBehind = skipIntroLengths.values.map {
                        Item(it, MR.strings.backup_conversion_skip_intro_length)
                    } + seasons.changedViewTitles.map { Item(it, MR.strings.backup_conversion_season_view) } +
                        audioLanguages.values,
                ),
            ),
        )
    }

    /** Animetail's audio language for the show, which Katari has no setting for (D071), or null where it's the app's. */
    private fun AniyomiAnime.audioLanguageItem(title: String): Item? = when (audioLanguage) {
        "" -> null
        ANIMETAIL_ORIGINAL_AUDIO -> Item(title, MR.strings.backup_conversion_audio_original)
        else -> Item("$title ($audioLanguage)", MR.strings.backup_conversion_audio_language)
    }

    // Manga and anime sources can share ids, so the type tells their entries apart
    private data class EntryKey(val type: EntryType, val source: Long, val url: String)
}

// Animetail's Anime.AUDIO_LANGUAGE_ORIGINAL
private const val ANIMETAIL_ORIGINAL_AUDIO = "original"
