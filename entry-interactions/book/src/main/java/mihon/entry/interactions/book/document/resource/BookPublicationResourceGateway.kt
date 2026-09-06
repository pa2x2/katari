package mihon.entry.interactions.book.document.resource

import android.app.Application
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.book.preparation.BookRemoteResourceAuthorization
import mihon.entry.interactions.book.preparation.BookRemoteResourceReference
import mihon.entry.interactions.book.preparation.BookRemoteResourceRequest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Consent-enforcing request and redirect gateway for passive publication resources. */
internal class BookPublicationResourceGateway(
    private val packaged: BookPublicationResourceLoader,
    references: Collection<BookRemoteResourceReference>,
    httpClient: OkHttpClient,
    snapshotDirectory: File,
) : BookPublicationResourceLoader, BookRemoteResourceAuthorization {
    private val referencesById = references.associateBy(BookRemoteResourceReference::resourceId)
    private val client = httpClient.newBuilder().followRedirects(false).followSslRedirects(false).build()
    private val snapshotDirectory = snapshotDirectory.also { directory ->
        check(directory.mkdirs() || directory.isDirectory) { "Unable to create publication resource snapshots" }
    }

    private val mutableGeneration = MutableStateFlow(0)
    override val generation = mutableGeneration.asStateFlow()

    @Volatile
    private var approvedOrigins = emptySet<String>()

    override val remoteResourceRequests = referencesById.values
        .mapTo(linkedSetOf()) { reference -> BookRemoteResourceRequest(reference.url.origin(), reference.type) }

    override fun authorizeRemoteOrigins(origins: Set<String>) {
        require(origins.all { origin -> remoteResourceRequests.any { it.origin == origin } }) {
            "Publication resource approval contains an undeclared origin"
        }
        if (approvedOrigins != origins) {
            approvedOrigins = origins.toSet()
            mutableGeneration.value += 1
        }
    }

    override suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookPublicationResource> {
        val reference = referencesById[resourceId]
            ?: return packaged.load(resourceId, acceptedMediaTypes, maxBytes)
        return try {
            require(reference.url.origin() in approvedOrigins) { "Remote publication resource has not been approved" }
            val snapshot = loadSnapshot(reference, acceptedMediaTypes, maxBytes)
            Result.success(snapshot ?: fetch(reference, acceptedMediaTypes, maxBytes))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private suspend fun fetch(
        reference: BookRemoteResourceReference,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): BookPublicationResource = withContext(Dispatchers.IO) {
        var url = reference.url.toHttpUrl()
        require(url.origin() in approvedOrigins) { "Remote publication resource has not been approved" }
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            require(url.origin() in approvedOrigins) {
                "Remote publication resource redirected to an unapproved origin"
            }
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val redirect = response.takeIf { it.code in REDIRECT_CODES }
                ?.header("Location")
                ?.let(url::resolve)
            if (redirect != null) {
                response.close()
                if (redirectCount == MAX_REDIRECTS) throw IOException("Too many publication resource redirects")
                if (url.isHttps && !redirect.isHttps) throw IOException("Publication resource redirected to HTTP")
                url = redirect
                return@repeat
            }
            response.use { completed ->
                if (!completed.isSuccessful) throw IOException("Publication resource request failed")
                val mediaType = completed.body.contentType()?.toString()?.substringBefore(';')?.lowercase()
                    ?: throw IOException("Publication resource omitted its media type")
                require(mediaType in acceptedMediaTypes) { "Publication resource returned an unsupported media type" }
                val bytes = completed.body.byteStream().readBounded(maxBytes)
                val resource = BookPublicationResource(reference.resourceId, mediaType, bytes)
                snapshot(reference, resource)
                return@withContext resource
            }
        }
        error("Unreachable publication resource redirect loop")
    }

    private fun loadSnapshot(
        reference: BookRemoteResourceReference,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): BookPublicationResource? {
        val base = snapshotDirectory.resolve(reference.snapshotKey())
        val bytesFile = File(base.path + ".bin")
        val mediaFile = File(base.path + ".type")
        val mediaType = mediaFile.takeIf(File::isFile)?.readText()?.trim()?.takeIf(acceptedMediaTypes::contains)
            ?: return null
        if (!bytesFile.isFile || bytesFile.length() !in 1..maxBytes.toLong()) return null
        return BookPublicationResource(reference.resourceId, mediaType, bytesFile.readBytes())
    }

    private fun snapshot(reference: BookRemoteResourceReference, resource: BookPublicationResource) {
        runCatching {
            val base = snapshotDirectory.resolve(reference.snapshotKey())
            File(base.path + ".bin").writeBytes(resource.bytes)
            File(base.path + ".type").writeText(requireNotNull(resource.mediaType))
        }
    }

    private fun java.io.InputStream.readBounded(maxBytes: Int): ByteArray = use { input ->
        val output = java.io.ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= maxBytes) { "Publication resource exceeds its byte limit" }
            output.write(buffer, 0, read)
        }
        require(total > 0) { "Publication resource is empty" }
        output.toByteArray()
    }

    private fun BookRemoteResourceReference.snapshotKey(): String = sha256(resourceId + "\u0000" + url)

    private companion object {
        const val MAX_REDIRECTS = 10
        val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
    }
}

internal class BookPublicationResourceGatewayFactory(
    application: Application,
    private val httpClient: OkHttpClient,
) {
    private val snapshotRoot = application.noBackupFilesDir.resolve("book_publication_resource_snapshots")

    fun create(
        publicationId: String,
        revision: String,
        packaged: BookPublicationResourceLoader,
        references: Collection<BookRemoteResourceReference>,
    ): BookPublicationResourceGateway = BookPublicationResourceGateway(
        packaged = packaged,
        references = references,
        httpClient = httpClient,
        snapshotDirectory = snapshotRoot.resolve(sha256(publicationId)).resolve(sha256(revision)),
    )

    fun removePublication(publicationId: String) {
        snapshotRoot.resolve(sha256(publicationId)).deleteRecursively()
    }
}

private fun String.origin(): String = toHttpUrl().origin()

private fun HttpUrl.origin(): String = "$scheme://$host:$port"

private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.encodeToByteArray())
    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
