package app.katari.webview.chromium

import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.descriptor.ModelArtifactFile
import app.katari.model.artifacts.api.descriptor.ModelArtifactHosting
import app.katari.model.artifacts.api.descriptor.ModelArtifactId
import app.katari.model.artifacts.api.descriptor.ModelArtifactLicense

/**
 * The one Chromium a build of the app runs: the Chromium of the JCEF it was built with, as the project publishes it.
 *
 * @property version the Chromium's version, which names its folder among the installed ones.
 * @property archiveBytes the size of the download.
 * @property installedBytes the size of the files the download unpacks to.
 */
class ChromiumRelease(
    val version: String,
    archiveUrl: String,
    archiveSha256: String,
    val archiveBytes: Long,
    val installedBytes: Long,
) {
    internal val archive = ModelArtifactDescriptor(
        id = ARCHIVE_ID,
        revision = version,
        displayName = "Chromium",
        files = listOf(ModelArtifactFile(ARCHIVE_NAME, archiveUrl, archiveBytes, archiveSha256)),
        license = ModelArtifactLicense("BSD 3-Clause and others", "https://www.chromium.org/chromium-os/licenses/"),
        hosting = ModelArtifactHosting.Project(
            sourceUrl = archiveUrl,
            upstreamUrl = "https://github.com/JetBrains/JetBrainsRuntime",
        ),
    )

    internal companion object {
        val ARCHIVE_ID = ModelArtifactId("chromium")
        const val ARCHIVE_NAME = "chromium.tar.zst"
    }
}
