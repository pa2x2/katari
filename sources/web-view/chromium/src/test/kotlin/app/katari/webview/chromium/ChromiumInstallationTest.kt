package app.katari.webview.chromium

import app.katari.model.artifacts.api.state.ModelArtifactFailure
import app.katari.model.artifacts.api.state.ModelArtifactState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

class ChromiumInstallationTest {
    @TempDir
    lateinit var root: Path

    private lateinit var store: FakeArchiveStore

    private fun TestScope.installation(
        failure: ModelArtifactState.Failed? = null,
        unpack: (Path, Path) -> Unit = { _, directory -> unpackServer(directory) },
    ) = ChromiumInstallation(
        release = ChromiumRelease(VERSION, "https://example.org/chromium.tar.zst", "0".repeat(64), 1, 1),
        root = root,
        store = { downloads -> FakeArchiveStore(downloads, failure).also { store = it } },
        scope = backgroundScope,
        io = StandardTestDispatcher(testScheduler),
        unpack = unpack,
    )

    @Test
    fun `an installation unpacks the download into its version's folder and removes the archive`() = runTest {
        val installation = installation()

        installation.install(allowMeteredNetwork = true)

        installation.state.first { it !is ChromiumState.Downloading && it != ChromiumState.Unpacking } shouldBe
            ChromiumState.Installed
        Files.isExecutable(root.resolve(VERSION).resolve("cef_server")) shouldBe true
        store.archive.exists() shouldBe false
    }

    @Test
    fun `an unpacking that fails leaves no version's folder and keeps the archive`() = runTest {
        val installation = installation { _, directory ->
            unpackServer(directory)
            throw IOException("disk full")
        }

        installation.install(allowMeteredNetwork = true)

        installation.state.first { it is ChromiumState.Failed } shouldBe
            ChromiumState.Failed(ChromiumFailure.Unpacking("disk full"))
        root.resolve(VERSION).exists() shouldBe false
        store.archive.exists() shouldBe true
    }

    @Test
    fun `a failed download fails the installation with the download's reason`() = runTest {
        val failure = ModelArtifactFailure.Network("no route")
        val installation = installation(failure = ModelArtifactState.Failed(failure))

        installation.install(allowMeteredNetwork = true)

        installation.state.first { it is ChromiumState.Failed } shouldBe
            ChromiumState.Failed(ChromiumFailure.Download(failure))
    }

    @Test
    fun `another version's folder is removed and told when it was installed`() = runTest {
        unpackServer(root.resolve("149.0.1-abc"))
        val installation = installation()

        installation.deleteOtherVersions() shouldBe true

        root.resolve("149.0.1-abc").exists() shouldBe false
        installation.deleteOtherVersions() shouldBe false
    }

    private companion object {
        const val VERSION = "150.0.2-def"

        fun unpackServer(directory: Path) {
            val server = directory.createDirectories().resolve("cef_server")
            server.writeText("server")
            server.toFile().setExecutable(true)
        }
    }
}
