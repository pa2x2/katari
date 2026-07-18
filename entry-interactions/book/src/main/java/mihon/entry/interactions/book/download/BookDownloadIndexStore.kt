package mihon.entry.interactions.book.download

import android.content.Context
import android.util.AtomicFile
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File

/** Persisted snapshot of the BOOK download index. Download packages remain the source of truth. */
@OptIn(ExperimentalSerializationApi::class)
internal class BookDownloadIndexStore(
    private val context: Context,
    private val cacheFile: File = File(context.cacheDir, CACHE_FILE_NAME),
) {
    fun read(downloadsRootUri: String?): List<VerifiedBookDownloadPackage>? {
        if (downloadsRootUri == null || !cacheFile.exists()) return null

        return runCatching {
            val snapshot = AtomicFile(cacheFile).openRead().use {
                ProtoBuf.decodeFromByteArray<BookDownloadIndexSnapshot>(it.readBytes())
            }
            require(snapshot.version == CURRENT_VERSION) { "Unsupported BOOK download index version" }
            require(snapshot.downloadsRootUri == downloadsRootUri) { "BOOK download root changed" }
            snapshot.packages.map { it.toPackage() }
        }.getOrElse {
            AtomicFile(cacheFile).delete()
            null
        }
    }

    fun write(downloadsRootUri: String?, packages: Collection<VerifiedBookDownloadPackage>) {
        if (downloadsRootUri == null) return

        val atomicFile = AtomicFile(cacheFile)
        var output: java.io.FileOutputStream? = null
        try {
            val snapshot = BookDownloadIndexSnapshot(
                downloadsRootUri = downloadsRootUri,
                packages = packages
                    .sortedWith(
                        compareBy<VerifiedBookDownloadPackage> { it.manifest.sourceId }
                            .thenBy { it.manifest.entryUrl }
                            .thenBy { it.manifest.childUrl },
                    )
                    .map { BookDownloadIndexPackage.fromPackage(it) },
            )
            val bytes = ProtoBuf.encodeToByteArray(snapshot)
            cacheFile.parentFile?.mkdirs()
            output = atomicFile.startWrite()
            output.write(bytes)
            atomicFile.finishWrite(output)
            output = null
        } catch (error: Throwable) {
            output?.let(atomicFile::failWrite)
            atomicFile.delete()
            throw error
        }
    }

    private fun BookDownloadIndexPackage.toPackage(): VerifiedBookDownloadPackage {
        val directory = UniFile.fromUri(context, directoryUri.toUri())
            ?: error("Unable to restore BOOK package directory")
        val expectedResourceIds = manifest.resources.mapTo(linkedSetOf(), BookDownloadedResource::id)
        require(resourceUris.keys == expectedResourceIds) { "BOOK download index resources do not match its manifest" }
        val resources = resourceUris.mapValues { (_, uri) ->
            UniFile.fromUri(context, uri.toUri()) ?: error("Unable to restore BOOK package resource")
        }
        return VerifiedBookDownloadPackage(directory, manifest, resources)
    }

    private companion object {
        const val CACHE_FILE_NAME = "book_dl_index_cache_v1"
        const val CURRENT_VERSION = 1
    }
}

@Serializable
private data class BookDownloadIndexSnapshot(
    val version: Int = 1,
    val downloadsRootUri: String,
    val packages: List<BookDownloadIndexPackage>,
)

@Serializable
private data class BookDownloadIndexPackage(
    val manifest: BookDownloadManifest,
    val directoryUri: String,
    val resourceUris: Map<String, String>,
) {
    companion object {
        fun fromPackage(download: VerifiedBookDownloadPackage) = BookDownloadIndexPackage(
            manifest = download.manifest,
            directoryUri = download.directory.uri.toString(),
            resourceUris = download.resources.mapValues { (_, file) -> file.uri.toString() },
        )
    }
}
