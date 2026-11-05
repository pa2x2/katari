package app.katari.sync.server.retention

import app.katari.sync.protocol.file.FileCategory
import app.katari.sync.protocol.file.FileClaimRequest
import app.katari.sync.protocol.file.FileReference
import app.katari.sync.server.TestServer
import app.katari.sync.server.file.UploadOutcome
import app.katari.sync.server.record
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class UnreferencedFileRetentionTest {

    @Test
    fun `a file a live record lists is kept however old, and goes a day after the record stops listing it`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val device = server.device(account)
            val cover = server.upload(account, "custom cover")
            device.push(record("entry:1", basedOn = 0).copy(files = listOf(cover)))
            val retention = UnreferencedFileRetention(server.services.storage, server.services.content, server.clock)

            server.clock.advance(30.days)
            retention.sweep()
            server.services.files.download(account, cover.category, cover.sha256).shouldNotBeNull()

            device.push(record("entry:1", basedOn = 1, deleted = true))
            retention.sweep()
            server.services.files.download(account, cover.category, cover.sha256).shouldBeNull()
        }
    }

    @Test
    fun `a file no record lists yet survives a day from its upload or last claim`(@TempDir dataDirectory: Path) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val cover = server.upload(account, "cover still being pushed")
            val retention = UnreferencedFileRetention(server.services.storage, server.services.content, server.clock)

            server.clock.advance(23.hours)
            retention.sweep()
            server.services.files.claim(account, FileClaimRequest(listOf(cover))).missing shouldBe emptyList()

            server.clock.advance(23.hours)
            retention.sweep()
            server.services.files.download(account, cover.category, cover.sha256).shouldNotBeNull()

            server.clock.advance(2.hours)
            retention.sweep()
            server.services.files.download(account, cover.category, cover.sha256).shouldBeNull()
            server.services.files.claim(account, FileClaimRequest(listOf(cover))).missing shouldContainExactly
                listOf(cover)
        }
    }

    private fun TestServer.upload(accountId: Long, content: String): FileReference {
        val bytes = content.encodeToByteArray()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()
        services.files.upload(accountId, FileCategory.COVER, sha256, bytes.inputStream()) shouldBe UploadOutcome.Stored
        return FileReference(FileCategory.COVER, sha256, bytes.size.toLong())
    }
}
