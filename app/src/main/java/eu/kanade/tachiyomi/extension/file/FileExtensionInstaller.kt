package eu.kanade.tachiyomi.extension.file

import android.content.Context
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.ListedDownload
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.util.lang.Hash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import logcat.LogPriority
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException

/**
 * Installs or updates a file extension from a store's listing, in the layout [FileExtensionStorage] describes. The
 * downloaded code is pinned by its SHA-256, which the loader checks before every run.
 */
internal class FileExtensionInstaller(
    private val context: Context,
    private val storage: FileExtensionStorage,
    private val client: () -> OkHttpClient,
) {

    /**
     * Throws with what stopped it, leaving any installed version as it was. [onInstalling] is called once the download
     * is done.
     *
     * @return whether it replaced an installed version.
     */
    suspend fun install(
        listing: Extension.Available,
        download: ListedDownload.File,
        onInstalling: () -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val (kind, id) = checkNotNull(ExtensionKinds.ofFilePkgName(listing.pkgName)) {
            "Katari doesn't run extensions like ${listing.name}"
        }
        download.companions.keys.forEach { name ->
            require(COMPANION_NAME.matches(name) && name !in RESERVED_NAMES) { "${listing.name} lists a file as $name" }
        }

        val code = fetch(download.url, MAX_CODE_BYTES)
        // Both are only shown, so the extension works without them
        val icon = fetchOrNull(listing.iconUrl, MAX_ICON_BYTES)
        val companions = download.companions.mapNotNull { (name, url) ->
            fetchOrNull(url, MAX_COMPANION_BYTES)?.let { name to it }
        }
        onInstalling()

        val sha256 = Hash.sha256(code)
        val manifest = FileExtensionManifest(
            id = id,
            name = listing.name,
            lang = listing.lang,
            versionName = listing.versionName,
            libVersionName = listing.libVersionName,
            contentWarning = listing.contentWarning,
            storeIndexUrl = listing.store.indexUrl,
            fileUrl = download.url,
            iconUrl = listing.iconUrl,
            sha256 = sha256,
            companions = companions.map { (name, _) -> name },
        )
        val installDir = storage.installDir(kind, id)
        val replaces = storage.readInstalled(kind, id) != null
        val staging = File(installDir, "$sha256$STAGING_SUFFIX")
        try {
            staging.deleteRecursively()
            if (!staging.mkdirs()) throw IOException("Unable to create ${staging.path}")
            File(staging, InstalledFileExtension.CODE_FILE).writeBytes(code)
            icon?.let { File(staging, InstalledFileExtension.ICON_FILE).writeBytes(it) }
            companions.forEach { (name, bytes) -> File(staging, name).writeBytes(bytes) }
            kind.prepareInstall(context, InstalledFileExtension(kind, manifest, staging))

            val contentDir = File(installDir, sha256)
            contentDir.deleteRecursively()
            if (!staging.renameTo(contentDir)) throw IOException("Unable to move ${staging.name} into place")
            writeManifest(installDir, manifest)
        } finally {
            staging.deleteRecursively()
        }
        installDir.listFiles()
            .orEmpty()
            .filter { it.name != sha256 && it.name != FileExtensionStorage.MANIFEST_FILE }
            .forEach { it.deleteRecursively() }
        replaces
    }

    /** Writing it replaces the old one at once, so a crash leaves either version installed. */
    private fun writeManifest(installDir: File, manifest: FileExtensionManifest) {
        val target = File(installDir, FileExtensionStorage.MANIFEST_FILE)
        val staged = File(installDir, "${target.name}$STAGING_SUFFIX")
        staged.writeText(FileExtensionStorage.json.encodeToString(manifest))
        if (!staged.renameTo(target)) {
            staged.delete()
            throw IOException("Unable to write ${target.path}")
        }
    }

    private suspend fun fetchOrNull(url: String, maxBytes: Long): ByteArray? {
        if (url.isEmpty()) return null
        return try {
            fetch(url, maxBytes)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logcat(LogPriority.WARN, e) { "Skipped $url" }
            null
        }
    }

    private suspend fun fetch(url: String, maxBytes: Long): ByteArray {
        val request = Request.Builder().url(url).build()
        return client().newCall(request).await().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download $url (HTTP ${response.code})")
            val body = response.body
            if (body.contentLength() > maxBytes) throw IOException(tooLarge(url, maxBytes))
            // Interruptible so cancelling the install stops the body read too
            runInterruptible {
                val buffer = Buffer()
                body.source().use { source ->
                    var read = 0L
                    while (buffer.size <= maxBytes && read != -1L) read = source.read(buffer, READ_CHUNK)
                }
                if (buffer.size > maxBytes) throw IOException(tooLarge(url, maxBytes))
                buffer.readByteArray()
            }
        }
    }

    private fun tooLarge(url: String, maxBytes: Long) = "$url is larger than ${maxBytes / 1024} KiB"

    private companion object {
        // Scripts are held in memory to run, and the largest LNReader plugin is about 210 KB
        const val MAX_CODE_BYTES = 4L * 1024 * 1024
        const val MAX_ICON_BYTES = 1L * 1024 * 1024
        const val MAX_COMPANION_BYTES = 1L * 1024 * 1024
        const val READ_CHUNK = 8192L
        const val STAGING_SUFFIX = ".part"

        val COMPANION_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
        val RESERVED_NAMES = setOf(
            InstalledFileExtension.CODE_FILE,
            InstalledFileExtension.ICON_FILE,
            FileExtensionStorage.MANIFEST_FILE,
        )
    }
}
