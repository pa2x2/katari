package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents
import eu.kanade.tachiyomi.data.backup.conversion.BackupFormat
import eu.kanade.tachiyomi.data.backup.conversion.KatariDefaultCategory
import eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings.animeSourceIdsInSettings
import eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings.convertAniyomiSettings
import eu.kanade.tachiyomi.data.backup.conversion.isKept
import eu.kanade.tachiyomi.data.backup.conversion.mihon.ConvertedCategories
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonManga
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonMerges
import eu.kanade.tachiyomi.data.backup.conversion.mihon.convertSavedSearches
import eu.kanade.tachiyomi.data.backup.conversion.mihon.hasProgress
import eu.kanade.tachiyomi.data.backup.conversion.mihon.mihonEntryShape
import eu.kanade.tachiyomi.data.backup.conversion.mihon.toBackupEntry
import eu.kanade.tachiyomi.data.backup.conversion.mihon.toBackupExtensionStore
import eu.kanade.tachiyomi.data.backup.conversion.newer.MessageShape
import eu.kanade.tachiyomi.data.backup.conversion.newer.NewerAppReading
import eu.kanade.tachiyomi.data.backup.conversion.newer.protobufEntry
import eu.kanade.tachiyomi.data.backup.conversion.newer.protobufPart
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.lengthDelimitedTag
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.BufferedSource

/**
 * Aniyomi's backups, in the layout of Aniyomi 0.18.1 and later and in the one before it, and those of the forks that keep
 * its fields: Animetail, Anikku and Animiru.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class AniyomiBackupFormat(
    private val context: PlatformContext,
    private val parser: ProtoBuf,
    private val json: Json,
) : BackupFormat {
    override val appName = "Aniyomi"

    override val fileReads = 3

    override val relatedAppNames = listOf("Animetail", "Anikku", "Animiru")

    // Manga alone is Mihon's layout, so a backup is Aniyomi's by its anime part; fields Katari doesn't know besides
    // it are a newer app's (D123)
    override fun recognizes(contents: BackupFileContents): Boolean =
        contents is BackupFileContents.Protobuf && contents.fieldNumbers.any { it in ANIME_FIELD_NUMBERS }

    override fun appWriting(contents: BackupFileContents): String =
        (contents as? BackupFileContents.Protobuf)?.let(::aniyomiFork) ?: appName

    override suspend fun convert(
        open: () -> BufferedSource,
        app: String,
        writeEntry: (BackupEntry) -> Unit,
    ): BackupFormat.ConvertedContent {
        val layout = open().use(AniyomiLayout::of)
        val reading = NewerAppReading(app)
        // Entries name sources, categories and parents that come after them, and Anikku's merged anime can come after
        // their members, which are written with their merge: everything else, and the merges, are read first
        val rest = Buffer()
        val mergeCollector = MihonMerges.Collector(parser, reading, EntryType.ANIME)
        val mangaSourceIds = hashSetOf<Long>()
        val seasonInfos = mutableListOf<AniyomiSeasonInfo>()
        var extensionApks = 0
        open().use { content ->
            val input = ProtoInput(content)
            while (!input.exhausted()) {
                when (val tag = input.readTag()) {
                    // An entry that doesn't decode is left behind when the entries are converted
                    MANGA_TAG -> input.readPart(reading, AniyomiEntrySource.serializer())
                        ?.let { mangaSourceIds += it.source }
                    layout.animeTag -> {
                        val anime = input.readByteArray(input.readLength())
                        reading.protobufPart(parser, AniyomiSeasonInfo.serializer(), anime)?.let { seasonInfos += it }
                        mergeCollector.add(anime)
                    }
                    // Extension APKs are left behind, and holding them would take the whole of each
                    layout.extensionApkTag -> {
                        input.skipField(tag)
                        extensionApks++
                    }
                    else -> input.copyField(tag, rest)
                }
            }
        }
        val merges = mergeCollector.build()
        val backup = layout.read(reading, parser, rest.readByteArray())
        val mangaShape = mihonEntryShape()
        val animeShape = aniyomiAnimeShape()
        val mergedCategories =
            ConvertedCategories(KatariDefaultCategory.of(context, app), backup.mangaCategories, backup.animeCategories)
        val seasons = AniyomiSeasons(seasonInfos)
        val animeSourceNames = backup.animeSources.associate { it.sourceId to it.name }
        // Anikku is anime-only, so its saved searches and feeds name anime sources
        val searchSources = backup.savedSearches.map { it.source } +
            backup.feeds.flatMap { listOfNotNull(it.source, it.savedSearch?.source) }
        val sourceKeys = AniyomiSourceKeys(
            mangaSources = backup.mangaSources,
            mangaSourceIds = mangaSourceIds + backup.mangaSources.map { it.sourceId },
            animeSources = (
                animeSourceNames.keys + seasonInfos.map { it.source } + searchSources +
                    animeSourceIdsInSettings(backup.preferences)
                )
                .filterNot(merges::isMergedEntry)
                .associateWith { animeSourceNames[it].orEmpty() },
        )
        val settings = convertAniyomiSettings(
            backup.preferences,
            app,
            animeCategoryId = { mergedCategories.idOf(ANIME_LIST, it) },
            animeSourceKey = sourceKeys::animeKey,
        )
        val savedSearches = convertSavedSearches(
            backup.savedSearches,
            backup.feeds,
            animeSourceNames,
            json,
            sourceKeys::animeKey,
        )
        val report = AniyomiConversionReport(merges)
        open().use { content ->
            val input = ProtoInput(content)
            while (!input.exhausted()) {
                when (val tag = input.readTag()) {
                    MANGA_TAG -> {
                        val manga = input.readEntry(reading, mangaShape, MihonManga.serializer()) ?: continue
                        val entry = manga.toBackupEntry()
                        if (!entry.isKept(manga.hasProgress)) {
                            report.addNotKept(manga)
                            continue
                        }
                        report.add(manga)
                        entry.categories = mergedCategories.of(MANGA_LIST, manga.categories)
                        writeEntry(entry)
                    }
                    layout.animeTag -> {
                        val anime = input.readEntry(reading, animeShape, AniyomiAnime.serializer()) ?: continue
                        if (merges.isMergedEntry(anime.source)) {
                            report.add(anime)
                            continue
                        }
                        val entry = anime.toBackupEntry(
                            source = sourceKeys.animeKey(anime.source),
                            categories = seasons.categoriesOf(anime),
                        )
                        seasons.applyTo(entry, anime.source, anime.url, sourceKeys::animeKey)
                        // A merge's members take the merged entry's categories, which are orders in the list too
                        merges.applyTo(entry, anime.source, sourceKeys::animeKey)
                        if (!entry.isKept(anime.hasProgress)) {
                            report.addNotKept(anime)
                            continue
                        }
                        report.add(anime)
                        entry.categories = mergedCategories.of(ANIME_LIST, entry.categories)
                        writeEntry(entry)
                    }
                    else -> input.skipField(tag)
                }
            }
        }
        val metadata = Backup(
            backupCategories = mergedCategories.backupCategories,
            backupSources = sourceKeys.backupSources,
            backupPreferences = settings.preferences + savedSearches.preferences,
            // Extensions define their own settings, which Katari keeps for them as they are
            backupSourcePreferences = sourceKeys.withAnimeKeys(backup.sourcePreferences),
            backupExtensionStores = backup.extensionSources.mapNotNull { it.toBackupExtensionStore() },
        )
        return BackupFormat.ConvertedContent(
            metadata,
            report.build(backup, mergedCategories, seasons, settings, savedSearches, extensionApks, sourceKeys),
            reading.findings(),
        )
    }

    private fun <T> ProtoInput.readEntry(
        reading: NewerAppReading,
        shape: MessageShape,
        deserializer: DeserializationStrategy<T>,
    ): T? = reading.protobufEntry(parser, shape, deserializer, readByteArray(readLength()))

    private fun <T> ProtoInput.readPart(reading: NewerAppReading, deserializer: DeserializationStrategy<T>): T? =
        reading.protobufPart(parser, deserializer, readByteArray(readLength()))

    private companion object {
        val MANGA_TAG = lengthDelimitedTag(1)

        // The order of the category lists given to ConvertedCategories
        const val MANGA_LIST = 0
        const val ANIME_LIST = 1

        /**
         * Where the anime part is in either layout: 3, 4 and 103 in the older one, 500-506 in the newer one. The
         * older layout's 107-109 are left out, as Mihon would add fields there next (MihonBackupFormat).
         */
        val ANIME_FIELD_NUMBERS = setOf(3, 4, 103) + (500..506)
    }
}
