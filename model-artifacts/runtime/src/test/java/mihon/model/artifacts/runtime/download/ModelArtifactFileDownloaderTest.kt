package mihon.model.artifacts.runtime.download

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.model.artifacts.runtime.artifactFile
import mihon.model.artifacts.runtime.artifactHttpClient
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModelArtifactFileDownloaderTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `interrupted downloads resume from the stored partial content`() = runTest {
        val content = "encoder weights".toByteArray()
        val file = artifactFile("encoder.onnx", content)
        val target = File(directory, file.name)
        File(directory, "encoder.onnx.part").writeBytes(content.copyOfRange(0, 8))

        MockWebServer().apply { start() }.use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .code(206)
                    .body(Buffer().write(content.copyOfRange(8, content.size)))
                    .build(),
            )

            val result = ModelArtifactFileDownloader { server.artifactHttpClient() }.download(file, target) {}

            result shouldBe ModelArtifactFileDownloadResult.Completed
            target.readBytes().toList() shouldBe content.toList()
            server.takeRequest().headers["Range"] shouldBe "bytes=8-"
        }
    }

    @Test
    fun `a server that ignores the range replaces the partial content`() = runTest {
        val content = "decoder weights".toByteArray()
        val file = artifactFile("decoder.onnx", content)
        val target = File(directory, file.name)
        File(directory, "decoder.onnx.part").writeBytes("stale".toByteArray())

        MockWebServer().apply { start() }.use { server ->
            server.enqueue(MockResponse.Builder().body(Buffer().write(content)).build())

            val result = ModelArtifactFileDownloader { server.artifactHttpClient() }.download(file, target) {}

            result shouldBe ModelArtifactFileDownloadResult.Completed
            target.readBytes().toList() shouldBe content.toList()
        }
    }

    @Test
    fun `content that does not match its digest is never moved into place`() = runTest {
        val file = artifactFile("vocab.txt", "expected".toByteArray())
        val target = File(directory, file.name)

        MockWebServer().apply { start() }.use { server ->
            server.enqueue(MockResponse.Builder().body(Buffer().write("tampered".toByteArray())).build())

            val result = ModelArtifactFileDownloader { server.artifactHttpClient() }.download(file, target) {}

            result shouldBe ModelArtifactFileDownloadResult.ChecksumMismatch
            target.exists() shouldBe false
            File(directory, "vocab.txt.part").exists() shouldBe false
        }
    }
}
