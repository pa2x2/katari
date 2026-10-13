package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Test

class MihonBackupProtoCompatibilityTest {

    @Test
    fun `Mihon memo and next update decode without becoming a display name or merge target`() {
        val emptyMemo = "{}".encodeToByteArray()
        val sourceMemo = """{"source":"state"}""".encodeToByteArray()
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = MihonBackup.serializer(),
            MihonBackup(
                backupManga = listOf(
                    MihonBackupManga(source = 1, url = "empty", memo = emptyMemo),
                    MihonBackupManga(
                        source = 1,
                        url = "source-state",
                        memo = sourceMemo,
                        nextUpdate = 1_790_000_000_000,
                    ),
                ),
            ),
        )

        val entries = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
            .allEntries()
            .associateBy { it.url }

        entries.getValue("empty").displayName shouldBe null
        entries.getValue("empty").memo.contentEquals(emptyMemo) shouldBe true
        entries.getValue("empty").nextUpdate shouldBe 0
        val scheduled = entries.getValue("source-state")
        scheduled.displayName shouldBe null
        scheduled.memo.contentEquals(sourceMemo) shouldBe true
        scheduled.nextUpdate shouldBe 1_790_000_000_000
        scheduled.mergeTargetSource shouldBe null
    }

    @Test
    fun `legacy Katari display name, memo and merge target survive decoding`() {
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
                    LegacyKatariBackupManga(
                        source = 1,
                        url = "merged",
                        displayName = "Shown name",
                        mergeTargetSource = 7,
                        mergeTargetUrl = "target",
                        mergePosition = 2,
                    ),
                ),
            ),
        )

        val entries = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
            .allEntries()
            .associateBy { it.url }

        entries.getValue("legacy").displayName shouldBe "{}"
        entries.getValue("legacy").memo.contentEquals(memo) shouldBe true
        val merged = entries.getValue("merged")
        merged.displayName shouldBe "Shown name"
        merged.mergeTargetSource shouldBe 7
        merged.mergeTargetUrl shouldBe "target"
        merged.mergePosition shouldBe 2
        merged.nextUpdate shouldBe 0
    }

    /** Field numbers of Mihon v0.21.0's BackupManga. */
    @Serializable
    private data class MihonBackup(
        @ProtoNumber(1) val backupManga: List<MihonBackupManga>,
    )

    @Serializable
    private data class MihonBackupManga(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(112) val memo: ByteArray,
        @ProtoNumber(113) val nextUpdate: Long = 0,
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
        @ProtoNumber(113) val mergeTargetSource: Long? = null,
        @ProtoNumber(114) val mergeTargetUrl: String? = null,
        @ProtoNumber(115) val mergePosition: Int? = null,
        @ProtoNumber(116) val memo: ByteArray? = null,
    )
}
