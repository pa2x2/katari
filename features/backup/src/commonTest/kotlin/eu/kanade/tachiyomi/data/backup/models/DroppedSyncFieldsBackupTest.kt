package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.backup.stream.readBackup
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Test

class DroppedSyncFieldsBackupTest {

    @Test
    fun `backups with the sync fields still restore, and a favorite without a date added takes its favorite time`() {
        val bytes = ProtoBuf { encodeDefaults = true }.encodeToByteArray(
            serializer = OldKatariBackup.serializer(),
            OldKatariBackup(
                backupEntries = listOf(
                    OldKatariBackupEntry(
                        url = "/undated",
                        dateAdded = 0,
                        chapters = listOf(OldKatariBackupChapter(url = "/1")),
                    ),
                    OldKatariBackupEntry(url = "/dated", dateAdded = 1_700_000_000_000),
                ),
            ),
        )

        val entries = readBackup(bytes).decodedEntries.associateBy { it.url }

        entries.getValue("/undated").toEntry().dateAdded shouldBe 1_600_000_000_000
        entries.getValue("/undated").chapters.single().read shouldBe true
        entries.getValue("/dated").toEntry().dateAdded shouldBe 1_700_000_000_000
    }

    /** Field numbers of Katari's backups before the sync fields were dropped. */
    @Serializable
    private data class OldKatariBackup(
        @ProtoNumber(203) val backupEntries: List<OldKatariBackupEntry>,
    )

    @Serializable
    private data class OldKatariBackupEntry(
        @ProtoNumber(1) val source: Long = 1,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(10) val dateAdded: Long,
        @ProtoNumber(11) val chapters: List<OldKatariBackupChapter> = emptyList(),
        @ProtoNumber(14) val favorite: Boolean = true,
        @ProtoNumber(19) val lastModifiedAt: Long = 1_650_000_000,
        @ProtoNumber(20) val favoriteModifiedAt: Long = 1_600_000_000,
        @ProtoNumber(22) val version: Long = 4,
    )

    @Serializable
    private data class OldKatariBackupChapter(
        @ProtoNumber(1) val url: String,
        @ProtoNumber(2) val name: String = "Chapter",
        @ProtoNumber(4) val read: Boolean = true,
        @ProtoNumber(11) val lastModifiedAt: Long = 1_650_000_000,
        @ProtoNumber(12) val version: Long = 2,
    )
}
