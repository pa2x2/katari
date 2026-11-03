package eu.kanade.tachiyomi.data.backup.stream

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupAnime
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupManga
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.feature.profiles.core.ProfileBackup
import mihon.feature.profiles.core.ProfileScopedBackup
import okio.Buffer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BackupStreamTest {

    @Test
    fun `a backup encoded whole reads entry by entry, each in its profile, with everything else intact`() {
        val backup = backup()

        val read = readBackup(encode(backup))

        read.entries.map { it.profileIndex to it.decode().url } shouldContainExactly listOf(
            null to "/top/manga",
            0 to "/first/manga",
            0 to "/first/anime",
            0 to "/first/entry",
            1 to "/second/entry",
            null to "/top/anime",
            null to "/top/entry",
        )
        encode(read.summary.metadata) shouldBe encode(backup.withoutEntries())
        read.summary.topLevelEntryCount shouldBe 3
        read.summary.profileEntryCounts shouldContainExactly listOf(3, 1)
        read.summary.entrySources shouldBe setOf(1L, 2L, 3L, 4L)
        read.summary.entrySourceKeys shouldBe setOf(1L, 2L, 3L, 4L, 9L)
        read.summary.trackingServiceIds shouldBe setOf(2L, 3L)
    }

    @Test
    fun `a backup written entry by entry decodes whole as the same backup`(@TempDir scratchDir: File) = runTest {
        val backup = Backup(
            backupCategories = listOf(BackupCategory(name = "Reading", order = 0)),
            backupProfiles = listOf(
                profile("first", entries = listOf(entry(1, "/a"), entry(2, "/b"))),
                profile("empty"),
            ),
            activeProfileUuid = "first",
            kitsuScoresNative = true,
        )
        val sink = Buffer()
        val writer = BackupWriter(sink, ProtoBuf, scratchDir)

        backup.backupProfiles.forEach { profile ->
            writer.writeProfile(profile) { writeEntry -> profile.entries.forEach(writeEntry) }
        }
        writer.writeMetadata(backup)
        val written = sink.readByteArray()

        encode(ProtoBuf.decodeFromByteArray(Backup.serializer(), written)) shouldBe encode(backup)
        readBackup(written).entries.map { it.profileIndex to it.decode().url } shouldContainExactly listOf(
            0 to "/a",
            0 to "/b",
        )
        scratchDir.listFiles().orEmpty().toList() shouldBe emptyList()
    }

    @Test
    fun `damaged backups fail as malformed`() {
        val bytes = encode(backup())
        val profile = ProtoBuf.encodeToByteArray(ProfileScopedBackup.serializer(), profile("p", listOf(entry(1, "/a"))))

        listOf(
            // Cut short
            bytes.copyOf(bytes.size / 2),
            bytes.copyOf(bytes.size - 1),
            // Wire type 7 doesn't exist
            byteArrayOf(0x0F),
            // Field number 0
            byteArrayOf(0x02, 0x00),
            // A varint that never ends
            ByteArray(11) { 0xFF.toByte() },
            // A profile one byte shorter than its entry
            Buffer().apply {
                writeVarint(BackupLayout.PROFILE_TAG)
                writeVarint(profile.size - 1L)
                write(profile)
            }.readByteArray(),
            // An entry that isn't a BackupEntry
            byteArrayOf(0xDA.toByte(), 0x0C, 0x02, 0x0F, 0x00),
        ).forEach { input ->
            assertThrows<MalformedBackupException> { readBackup(input).decodedEntries }
        }
    }

    private fun Backup.withoutEntries() = copy(
        backupManga = emptyList(),
        backupAnime = emptyList(),
        backupEntries = emptyList(),
        backupProfiles = backupProfiles.map {
            it.copy(manga = emptyList(), anime = emptyList(), entries = emptyList())
        },
    )

    private fun backup() = Backup(
        backupManga = listOf(
            LegacyBackupManga(
                source = 1,
                url = "/top/manga",
                tracking = listOf(BackupTracking(syncId = 3, libraryId = 1)),
            ),
        ),
        backupCategories = listOf(BackupCategory(name = "Reading", order = 0)),
        backupSources = listOf(BackupSource(name = "One", sourceId = 1)),
        backupPreferences = listOf(BackupPreference("key", StringPreferenceValue("value"))),
        backupProfiles = listOf(
            ProfileScopedBackup(
                profile = profileBackup("first"),
                categories = listOf(BackupCategory(name = "Done", order = 1)),
                manga = listOf(LegacyBackupManga(source = 2, url = "/first/manga")),
                preferences = listOf(BackupPreference("profile_key", StringPreferenceValue("value"))),
                anime = listOf(LegacyBackupAnime(source = 3, url = "/first/anime")),
                entries = listOf(
                    entry(4, "/first/entry").apply {
                        mergeTargetSource = 9
                        tracking = listOf(BackupTracking(syncId = 2, libraryId = 1))
                    },
                ),
            ),
            profile("second", entries = listOf(entry(1, "/second/entry"))),
        ),
        activeProfileUuid = "first",
        backupAnime = listOf(LegacyBackupAnime(source = 3, url = "/top/anime")),
        backupEntries = listOf(
            entry(2, "/top/entry").apply {
                chapters = List(50) { BackupChapter(url = "/chapter/$it", name = "Chapter $it") }
            },
        ),
    )

    private fun profile(uuid: String, entries: List<BackupEntry> = emptyList()) =
        ProfileScopedBackup(profile = profileBackup(uuid), entries = entries)

    private fun profileBackup(uuid: String) = ProfileBackup(
        uuid = uuid,
        name = uuid,
        colorSeed = 1,
        position = 0,
        requiresAuth = false,
        isArchived = false,
    )

    private fun entry(source: Long, url: String) = BackupEntry(source = source, url = url, title = url)
}
