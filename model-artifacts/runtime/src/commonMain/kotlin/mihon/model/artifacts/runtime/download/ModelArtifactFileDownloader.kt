package mihon.model.artifacts.runtime.download

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Downloads one artifact file into place.
 *
 * Content is written to a `.part` sibling that survives interruption and is resumed with an HTTP range request. The
 * target file appears only after its size and SHA-256 digest match the declaration.
 */
internal class ModelArtifactFileDownloader(
    private val httpClient: () -> OkHttpClient,
) {
    suspend fun download(
        file: ModelArtifactFile,
        target: File,
        onProgress: (downloadedBytes: Long) -> Unit,
    ): ModelArtifactFileDownloadResult {
        if (target.isFile && target.length() == file.sizeBytes) {
            onProgress(file.sizeBytes)
            return ModelArtifactFileDownloadResult.Completed
        }
        target.parentFile?.mkdirs()
        val part = File(target.path + PART_SUFFIX)
        if (part.length() > file.sizeBytes) part.delete()

        if (part.length() < file.sizeBytes) {
            var outcome = transfer(file, part, onProgress)
            if (outcome == TransferOutcome.RangeRejected) {
                part.delete()
                outcome = transfer(file, part, onProgress)
            }
            when (outcome) {
                is TransferOutcome.Failed -> return outcome.result
                TransferOutcome.RangeRejected ->
                    return ModelArtifactFileDownloadResult.NetworkFailed("Server rejected a full download")
                TransferOutcome.Transferred -> Unit
            }
        }
        onProgress(part.length())

        if (part.length() != file.sizeBytes || part.sha256() != file.sha256) {
            part.delete()
            return ModelArtifactFileDownloadResult.ChecksumMismatch
        }
        if (!part.renameTo(target)) {
            return ModelArtifactFileDownloadResult.StorageFailed("Cannot move ${part.name} into place")
        }
        return ModelArtifactFileDownloadResult.Completed
    }

    private suspend fun transfer(
        file: ModelArtifactFile,
        part: File,
        onProgress: (downloadedBytes: Long) -> Unit,
    ): TransferOutcome {
        val resumeFrom = part.length()
        val request = Request.Builder()
            .url(file.url)
            .apply { if (resumeFrom > 0) header("Range", "bytes=$resumeFrom-") }
            .build()
        try {
            httpClient().newCall(request).execute().use { response ->
                val append = when {
                    response.code == HTTP_PARTIAL_CONTENT && resumeFrom > 0 -> true
                    response.code == HTTP_RANGE_NOT_SATISFIABLE && resumeFrom > 0 ->
                        return TransferOutcome.RangeRejected
                    response.isSuccessful -> false
                    else -> return failed(ModelArtifactFileDownloadResult.NetworkFailed("HTTP ${response.code}"))
                }
                var downloaded = if (append) resumeFrom else 0L
                onProgress(downloaded)
                val output = try {
                    FileOutputStream(part, append)
                } catch (error: IOException) {
                    return failed(ModelArtifactFileDownloadResult.StorageFailed(error.message))
                }
                output.use { sink ->
                    response.body.byteStream().use { source ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = source.read(buffer)
                            if (read < 0) break
                            if (downloaded + read > file.sizeBytes) {
                                part.delete()
                                return failed(ModelArtifactFileDownloadResult.ChecksumMismatch)
                            }
                            try {
                                sink.write(buffer, 0, read)
                            } catch (error: IOException) {
                                return failed(ModelArtifactFileDownloadResult.StorageFailed(error.message))
                            }
                            downloaded += read
                            onProgress(downloaded)
                        }
                    }
                }
            }
        } catch (error: IOException) {
            return failed(ModelArtifactFileDownloadResult.NetworkFailed(error.message))
        }
        return TransferOutcome.Transferred
    }

    private fun failed(result: ModelArtifactFileDownloadResult) = TransferOutcome.Failed(result)

    private sealed interface TransferOutcome {
        data object Transferred : TransferOutcome

        data object RangeRejected : TransferOutcome

        data class Failed(val result: ModelArtifactFileDownloadResult) : TransferOutcome
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val BUFFER_SIZE = 64 * 1024
        const val HTTP_PARTIAL_CONTENT = 206
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
    }
}

internal sealed interface ModelArtifactFileDownloadResult {
    data object Completed : ModelArtifactFileDownloadResult

    data object ChecksumMismatch : ModelArtifactFileDownloadResult

    data class NetworkFailed(val message: String?) : ModelArtifactFileDownloadResult

    data class StorageFailed(val message: String?) : ModelArtifactFileDownloadResult
}
