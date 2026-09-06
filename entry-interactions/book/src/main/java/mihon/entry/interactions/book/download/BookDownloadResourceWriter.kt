package mihon.entry.interactions.book.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.content.MaterializedBookResource
import mihon.entry.interactions.book.document.resource.validateBookDocumentResource
import mihon.entry.interactions.book.preparation.BookResourceRequirement
import java.io.IOException
import java.security.MessageDigest

/** Writes and describes the actual storage-provider file used by an offline resource. */
internal class BookDownloadResourceWriter(
    private val provider: BookDownloadProvider,
    private val staging: BookDownloadStagingPackage,
    private val budget: BookDownloadResourceBudgetTracker,
    private val requirements: Map<String, BookResourceRequirement>,
) {
    suspend fun write(resourceId: String, resource: MaterializedBookResource): BookDownloadedResource =
        withContext(Dispatchers.IO) {
            budget.include(resource.file.length(), resourceId)
            requirements[resourceId]?.let { requirement ->
                try {
                    resource.file.validateBookDocumentResource(resource.metadata.mediaType, requirement)
                } catch (error: Exception) {
                    resource.invalidate()
                    throw BookResourceValidationException(
                        "Required BOOK resource $resourceId is invalid: ${error.message}",
                        error,
                    )
                }
            }
            val requestedName = provider.resourceFileName(resource.metadata.id, resource.metadata.mediaType)
            val output = staging.directory.createFile(requestedName)
                ?: throw IOException("Unable to create downloaded BOOK resource")
            val actualName = output.name
                ?: throw IOException("Unable to determine downloaded BOOK resource filename")
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            resource.file.inputStream().buffered().use { input ->
                output.openOutputStream().buffered().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        target.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copied += read
                    }
                }
            }
            require(copied > 0L) { "Downloaded BOOK resource is empty" }
            BookDownloadedResource(
                id = resource.metadata.id,
                title = resource.metadata.title,
                order = resource.metadata.order,
                groupId = resource.metadata.groupId,
                mediaType = resource.metadata.mediaType,
                revision = resource.metadata.revision,
                fileName = actualName,
                storedSize = copied,
                sha256 = digest.digest().joinToString("") { byte ->
                    (byte.toInt() and 0xff).toString(16).padStart(2, '0')
                },
            )
        }
}
