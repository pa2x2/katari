package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Test

class MihonBackupProtoCompatibilityTest {

    @Test
    fun `current Mihon manga memo decodes as memo instead of display name`() {
        val emptyMemo = "{}".encodeToByteArray()
        val sourceMemo = """{"source":"state"}""".encodeToByteArray()
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = MihonBackup.serializer(),
            MihonBackup(
                backupManga = listOf(
                    MihonBackupManga(source = 1, url = "empty", memo = emptyMemo),
                    MihonBackupManga(source = 1, url = "source-state", memo = sourceMemo),
                ),
            ),
        )

        val entries = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
            .allEntries()
            .associateBy { it.url }

        entries.getValue("empty").displayName shouldBe null
        entries.getValue("empty").memo.contentEquals(emptyMemo) shouldBe true
        entries.getValue("source-state").displayName shouldBe null
        entries.getValue("source-state").memo.contentEquals(sourceMemo) shouldBe true
    }

    @Test
    fun `legacy Katari field 116 retains display name and memo`() {
        val memo = """{"legacy":true}""".encodeToByteArray()
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = LegacyKatariBackup.serializer(),
            LegacyKatariBackup(
                backupManga = listOf(
                    LegacyKatariBackupManga(
                        source = 1,
                        url = "legacy",
                        displayName = "{}",
                        memo = memo,
                    ),
                ),
            ),
        )

        val entry = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes).allEntries().single()

        entry.displayName shouldBe "{}"
        entry.memo.contentEquals(memo) shouldBe true
    }

    @Test
    fun `legacy Katari display name without extension fields remains readable`() {
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = LegacyKatariDisplayNameBackup.serializer(),
            LegacyKatariDisplayNameBackup(
                backupManga = listOf(
                    LegacyKatariDisplayNameManga(
                        source = 1,
                        url = "display-name-only",
                        displayName = "Custom title",
                    ),
                ),
            ),
        )

        val entry = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes).allEntries().single()

        entry.displayName shouldBe "Custom title"
        entry.memo.contentEquals("{}".encodeToByteArray()) shouldBe true
    }

    @Serializable
    private data class MihonBackup(
        @ProtoNumber(1) val backupManga: List<MihonBackupManga>,
    )

    @Serializable
    private data class MihonBackupManga(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(112) val memo: ByteArray,
    )

    @Serializable
    private data class LegacyKatariBackup(
        @ProtoNumber(1) val backupManga: List<LegacyKatariBackupManga>,
    )

    @Serializable
    private data class LegacyKatariBackupManga(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(112) val displayName: String,
        @ProtoNumber(116) val memo: ByteArray,
    )

    @Serializable
    private data class LegacyKatariDisplayNameBackup(
        @ProtoNumber(1) val backupManga: List<LegacyKatariDisplayNameManga>,
    )

    @Serializable
    private data class LegacyKatariDisplayNameManga(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(112) val displayName: String,
    )
}
