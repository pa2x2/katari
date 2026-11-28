package eu.kanade.tachiyomi.data.backup.conversion.mihon

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents
import eu.kanade.tachiyomi.data.backup.conversion.BackupFormat
import eu.kanade.tachiyomi.data.backup.conversion.newer.NewerAppReading
import eu.kanade.tachiyomi.data.backup.conversion.newer.protobufEntry
import eu.kanade.tachiyomi.data.backup.conversion.newer.protobufWhole
import eu.kanade.tachiyomi.data.backup.conversion.withTachiyomiIdentity
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.lengthDelimitedTag
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.BufferedSource

/**
 * Mihon's backups, and those of the apps that keep its fields: TachiyomiSY, Komikku, TachiyomiJ2K, Yōkai, Suwayomi and
 * Tachiyomi.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class MihonBackupFormat(
    private val parser: ProtoBuf,
    private val json: Json,
) : BackupFormat {
    override val appName = "Mihon"

    override val fileReads = 2

    override val relatedAppNames = listOf("TachiyomiSY", "Komikku", "TachiyomiJ2K", "Yōkai", "Suwayomi", "Tachiyomi")

    // A field the family doesn't know is a newer app's (D123), but Aniyomi's anime fields mean Aniyomi wrote the file
    override fun recognizes(contents: BackupFileContents): Boolean =
        contents is BackupFileContents.Protobuf &&
            contents.fieldNumbers.any { it in SIGNATURE } &&
            contents.fieldNumbers.none { it in ANIYOMI_SIGNATURE }

    override fun appWriting(contents: BackupFileContents): String =
        (contents as? BackupFileContents.Protobuf)?.let(::mihonFork) ?: appName

    override suspend fun convert(
        open: () -> BufferedSource,
        app: String,
        writeEntry: (BackupEntry) -> Unit,
    ): BackupFormat.ConvertedContent {
        // Categories come after the entries that name them, and merged entries can come after their members, which are
        // written with their merge: everything but the entries, and the merges among them, are read first
        val reading = NewerAppReading(app)
        val rest = Buffer()
        val mergeCollector = MihonMerges.Collector(parser, reading)
        open().use { content ->
            val input = ProtoInput(content)
            while (!input.exhausted()) {
                val tag = input.readTag()
                if (tag == ENTRY_TAG) {
                    mergeCollector.add(input.readByteArray(input.readLength()))
                } else {
                    input.copyField(tag, rest)
                }
            }
        }
        val merges = mergeCollector.build()
        val backup = reading.protobufWhole(parser, mihonBackupShape(), MihonBackup.serializer(), rest.readByteArray())
        val entryShape = mihonEntryShape()
        val categories = ConvertedCategories(backup.categories)
        val report = MihonConversionReport(merges)
        open().use { content ->
            val input = ProtoInput(content)
            while (!input.exhausted()) {
                val tag = input.readTag()
                if (tag != ENTRY_TAG) {
                    input.skipField(tag)
                    continue
                }
                val bytes = input.readByteArray(input.readLength())
                val manga = reading.protobufEntry(parser, entryShape, MihonManga.serializer(), bytes) ?: continue
                report.add(manga)
                if (merges.isMergedEntry(manga.source)) continue
                val entry = manga.toBackupEntry().also(merges::applyTo)
                entry.categories = categories.of(CATEGORY_LIST, entry.categories)
                writeEntry(entry)
            }
        }
        val settings = convertMihonSettings(backup.preferences, app)
        val sourceNames = backup.sources.associate { it.sourceId to it.name }
        val savedSearches =
            convertSavedSearches(backup.savedSearches, backup.syFeeds + backup.komikkuFeeds, sourceNames, json)
        val metadata = Backup(
            backupCategories = categories.backupCategories,
            backupSources = backup.sources
                .filter { it.sourceId != MihonMerges.MERGED_SOURCE_ID }
                .map { it.withTachiyomiIdentity() },
            backupPreferences = settings.preferences + savedSearches.preferences,
            // Extensions define their own settings, which Katari keeps for them as they are
            backupSourcePreferences = backup.sourcePreferences,
            backupExtensionStores = backup.extensionSources.mapNotNull { it.toBackupExtensionStore() },
        )
        return BackupFormat.ConvertedContent(
            metadata,
            report.build(backup, categories, settings, savedSearches),
            reading.findings(),
        )
    }

    private companion object {
        val ENTRY_TAG = lengthDelimitedTag(1)

        // The family keeps one category list
        const val CATEGORY_LIST = 0

        /** Top-level fields every app of the family writes: entries, categories, sources, settings, stores. */
        val SIGNATURE = setOf(1, 2, 101, 104, 105, 106)

        /**
         * Where Aniyomi keeps its anime: 3, 4 and 103 in its older layout, 500-506 in its newer one. Aniyomi's older
         * extension fields at 107-109 aren't among them, as they're where Mihon would add fields next; a manga-only
         * backup of that layout reads as Mihon's, with them as parts Katari doesn't know.
         */
        val ANIYOMI_SIGNATURE = setOf(3, 4, 103) + (500..506)
    }
}
