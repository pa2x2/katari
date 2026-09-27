package mihon.model.artifacts.runtime.store

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactFailure
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.runtime.artifactDescriptor
import mihon.model.artifacts.runtime.artifactFile
import mihon.model.artifacts.runtime.artifactHttpClient
import mihon.model.artifacts.runtime.download.ModelArtifactFileDownloader
import mihon.model.artifacts.runtime.network.ModelArtifactNetworkPolicy
import mihon.model.artifacts.runtime.storage.ModelArtifactStorage
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DefaultModelArtifactStoreTest {

    @TempDir
    lateinit var root: File

    private val encoder = "encoder".toByteArray()
    private val decoder = "decoder".toByteArray()
    private val descriptor = artifactDescriptor(
        artifactFile("encoder.onnx", encoder),
        artifactFile("decoder.onnx", decoder),
    )

    @Test
    fun `an approved download installs every file of the artifact`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            server.enqueue(MockResponse.Builder().body(Buffer().write(encoder)).build())
            server.enqueue(MockResponse.Builder().body(Buffer().write(decoder)).build())
            val store = store(server, metered = false)

            store.download(ModelArtifactDownloadApproval(descriptor, allowMeteredNetwork = false))

            val installed = store.observe(descriptor).first { it is ModelArtifactState.Installed }
                .shouldBeInstanceOf<ModelArtifactState.Installed>()
                .artifact
            installed.file("encoder.onnx").readBytes().toList() shouldBe encoder.toList()
            installed.file("decoder.onnx").readBytes().toList() shouldBe decoder.toList()
        }
    }

    @Test
    fun `metered networks are not used unless the approval allows them`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val store = store(server, metered = true)

            store.download(ModelArtifactDownloadApproval(descriptor, allowMeteredNetwork = false))

            store.observe(descriptor).first { it is ModelArtifactState.Failed } shouldBe
                ModelArtifactState.Failed(ModelArtifactFailure.MeteredNetwork)
            server.requestCount shouldBe 0
        }
    }

    @Test
    fun `deleting an artifact removes its installation and stored content`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            server.enqueue(MockResponse.Builder().body(Buffer().write(encoder)).build())
            server.enqueue(MockResponse.Builder().body(Buffer().write(decoder)).build())
            val store = store(server, metered = false)
            store.download(ModelArtifactDownloadApproval(descriptor, allowMeteredNetwork = false))
            store.observe(descriptor).first { it is ModelArtifactState.Installed }

            store.delete(descriptor.id)

            store.observe(descriptor).first() shouldBe ModelArtifactState.NotInstalled
            store.observeStored().first() shouldBe emptyList()
        }
    }

    private fun TestScope.store(server: MockWebServer, metered: Boolean): DefaultModelArtifactStore {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return DefaultModelArtifactStore(
            storage = ModelArtifactStorage { root },
            downloader = ModelArtifactFileDownloader(server.artifactHttpClient()),
            networkPolicy = ModelArtifactNetworkPolicy { metered },
            scope = backgroundScope,
            ioDispatcher = dispatcher,
        )
    }
}
