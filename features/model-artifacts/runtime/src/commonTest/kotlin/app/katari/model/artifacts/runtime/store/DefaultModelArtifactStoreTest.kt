package app.katari.model.artifacts.runtime.store

import app.katari.model.artifacts.api.download.ModelArtifactDownloadApproval
import app.katari.model.artifacts.api.state.ModelArtifactFailure
import app.katari.model.artifacts.api.state.ModelArtifactState
import app.katari.model.artifacts.runtime.artifactDescriptor
import app.katari.model.artifacts.runtime.artifactFile
import app.katari.model.artifacts.runtime.artifactHttpClient
import app.katari.model.artifacts.runtime.download.ModelArtifactFileDownloader
import app.katari.model.artifacts.runtime.network.ModelArtifactNetworkPolicy
import app.katari.model.artifacts.runtime.storage.ModelArtifactStorage
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DefaultModelArtifactStoreTest {

    @TempDir
    lateinit var root: File

    private val descriptor = artifactDescriptor(artifactFile("encoder.onnx", "encoder".toByteArray()))

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

    private fun TestScope.store(server: MockWebServer, metered: Boolean): DefaultModelArtifactStore {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return DefaultModelArtifactStore(
            storage = ModelArtifactStorage { root },
            downloader = ModelArtifactFileDownloader { server.artifactHttpClient() },
            networkPolicy = ModelArtifactNetworkPolicy { metered },
            scope = backgroundScope,
            ioDispatcher = dispatcher,
        )
    }
}
