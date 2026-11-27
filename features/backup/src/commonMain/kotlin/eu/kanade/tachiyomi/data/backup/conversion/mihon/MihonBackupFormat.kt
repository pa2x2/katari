package eu.kanade.tachiyomi.data.backup.conversion.mihon

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents
import eu.kanade.tachiyomi.data.backup.conversion.BackupFormat
import eu.kanade.tachiyomi.data.backup.conversion.withTachiyomiIdentity
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.decodeBackup
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

    override val relatedAppNames = listOf("TachiyomiSY", "Komikku", "TachiyomiJ2K", "Yōkai", "Suwayomi", "Tachiyomi")

    // A field outside the family's means another app wrote the file, such as Aniyomi's anime at 3 or 500 and up
    override fun recognizes(contents: BackupFileContents): Boolean =
        contents is BackupFileContents.Protobuf && contents.fieldNumbers.all { it in FIELD_NUMBERS }

    override suspend fun convert(
        open: () -> BufferedSource,
        writeEntry: (BackupEntry) -> Unit,
    ): BackupFormat.ConvertedContent {
        // Merged entries can come after their members, which are written with their merge
        val merges = open().use { MihonMerges.read(it, parser) }
        val report = MihonConversionReport(merges)
        val rest = Buffer()
        open().use { content ->
            val input = ProtoInput(content)
            while (!input.exhausted()) {
                val tag = input.readTag()
                if (tag != ENTRY_TAG) {
                    input.copyField(tag, rest)
                    continue
                }
                val manga = parser.decodeBackup(MihonManga.serializer(), input.readByteArray(input.readLength()))
                report.add(manga)
                if (merges.isMergedEntry(manga)) continue
                writeEntry(manga.toBackupEntry().also(merges::applyTo))
            }
        }
        val backup = parser.decodeBackup(MihonBackup.serializer(), rest.readByteArray())
        val settings = convertMihonSettings(backup.preferences)
        val sourceNames = backup.sources.associate { it.sourceId to it.name }
        val savedSearches =
            convertSavedSearches(backup.savedSearches, backup.syFeeds + backup.komikkuFeeds, sourceNames, json)
        val metadata = Backup(
            backupCategories = backup.categories.map { it.toBackupCategory() },
            backupSources = backup.sources
                .filter { it.sourceId != MihonMerges.MERGED_SOURCE_ID }
                .map { it.withTachiyomiIdentity() },
            backupPreferences = settings.preferences + savedSearches.preferences,
            // Extensions define their own settings, which Katari keeps for them as they are
            backupSourcePreferences = backup.sourcePreferences,
            backupExtensionStores = backup.extensionSources.mapNotNull { it.toBackupExtensionStore() },
        )
        return BackupFormat.ConvertedContent(metadata, report.build(backup, settings, savedSearches))
    }

    private companion object {
        val ENTRY_TAG = lengthDelimitedTag(1)

        /**
         * Every top-level field the family writes: Mihon's 1-106 (100 in old backups), TachiyomiSY's and Komikku's
         * saved searches and feeds at 600, 601 and 610, and Suwayomi's 9000-9002.
         */
        val FIELD_NUMBERS = setOf(1, 2, 100, 101, 104, 105, 106, 600, 601, 610, 9000, 9001, 9002)
    }
}
