package app.katari.webview.chromium

import app.katari.core.archive.unpackArchive
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.model.artifacts.api.download.ModelArtifactDownloadApproval
import app.katari.model.artifacts.api.state.ModelArtifactState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * The app's Chromium in its data folder: [release], downloaded and unpacked into a folder under [root] named after its
 * version, so a build that runs another Chromium finds its own missing and what is left of the older one to remove.
 * The folder exists only once every file is in it.
 *
 * [store] downloads the archive and must keep it under [downloadsDirectory]; once unpacked, the archive is removed.
 * [unpack] unpacks an archive into a folder.
 */
class ChromiumInstallation(
    val release: ChromiumRelease,
    root: Path,
    store: (downloads: File) -> ModelArtifactStore,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val unpack: (archive: Path, directory: Path) -> Unit = ::unpackArchive,
) {
    private val folders = ChromiumFolders(root, release.version)
    private val store by lazy { store(folders.downloads.toFile()) }

    /** Where the installed Chromium is, or will be. */
    val directory: Path = folders.installed

    private val mutableState = MutableStateFlow(
        if (folders.isInstalled) ChromiumState.Installed else ChromiumState.NotInstalled,
    )
    val state: StateFlow<ChromiumState> = mutableState.asStateFlow()

    private val lock = Any()
    private var installing: Job? = null

    /**
     * Installs Chromium unless it is installed or being installed, going on from what an earlier attempt downloaded.
     * [state] tells how it goes; it carries on when whoever asked goes away, until [cancel].
     */
    fun install(allowMeteredNetwork: Boolean) {
        synchronized(lock) {
            if (installing?.isActive == true || mutableState.value == ChromiumState.Installed) return
            mutableState.value = ChromiumState.Downloading(0, release.archiveBytes)
            installing = scope.launch(io) {
                // What a cancelled installation ends as: what it downloaded stays for the next attempt
                var result: ChromiumState = ChromiumState.NotInstalled
                try {
                    result = installNow(allowMeteredNetwork)
                } finally {
                    mutableState.value = result
                }
            }
        }
    }

    /** Stops an installation under way. */
    fun cancel() {
        synchronized(lock) { installing }?.cancel()
        store.cancel(release.archive)
    }

    /** Removes Chromium and anything downloaded of it. Chromium must not be running. */
    suspend fun delete() {
        synchronized(lock) { installing }?.cancelAndJoin()
        store.delete(ChromiumRelease.ARCHIVE_ID)
        withContext(io) { folders.deleteInstalled() }
        mutableState.value = ChromiumState.NotInstalled
    }

    /**
     * Removes what the Chromium of other builds of the app left behind; whether one of them was installed, in which
     * case this build's is what the user set up and lacks.
     */
    suspend fun deleteOtherVersions(): Boolean = withContext(io) {
        try {
            folders.deleteOtherVersions()
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "An older Chromium couldn't be removed" }
            false
        }
    }

    /** The disk space Chromium takes now: installed, downloaded in part, or both. */
    suspend fun storedBytes(): Long = withContext(io) { folders.storedBytes() }

    private suspend fun installNow(allowMeteredNetwork: Boolean): ChromiumState {
        if (folders.isInstalled) return ChromiumState.Installed
        try {
            if (!folders.runPrograms()) return ChromiumState.Failed(ChromiumFailure.FolderRunsNoPrograms)
            val required = release.archiveBytes + release.installedBytes - folders.storedBytes()
            val available = folders.usableBytes
            if (required > available) {
                return ChromiumState.Failed(ChromiumFailure.InsufficientStorage(required, available))
            }
        } catch (e: IOException) {
            return ChromiumState.Failed(ChromiumFailure.Unpacking(e.message))
        }

        store.download(ModelArtifactDownloadApproval(release.archive, allowMeteredNetwork))
        val downloaded = try {
            store.observe(release.archive)
                .onEach { if (it is ModelArtifactState.Downloading) mutableState.value = it.toChromiumState() }
                .first { it !is ModelArtifactState.Downloading }
        } finally {
            store.cancel(release.archive)
        }
        val archive = when (downloaded) {
            is ModelArtifactState.Installed -> downloaded.artifact.file(ChromiumRelease.ARCHIVE_NAME).toPath()
            is ModelArtifactState.Failed -> return ChromiumState.Failed(ChromiumFailure.Download(downloaded.failure))
            // The download was cancelled from elsewhere
            ModelArtifactState.NotInstalled, is ModelArtifactState.Downloading -> return ChromiumState.NotInstalled
        }

        mutableState.value = ChromiumState.Unpacking
        try {
            folders.install { directory -> unpack(archive, directory) }
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Chromium couldn't be unpacked" }
            return ChromiumState.Failed(ChromiumFailure.Unpacking(e.message))
        }
        // Unpacking can't be stopped half way, so an installation cancelled during it still ends installed
        withContext(NonCancellable) { store.delete(ChromiumRelease.ARCHIVE_ID) }
        return ChromiumState.Installed
    }

    private fun ModelArtifactState.Downloading.toChromiumState() =
        ChromiumState.Downloading(downloadedBytes, totalBytes)
}
