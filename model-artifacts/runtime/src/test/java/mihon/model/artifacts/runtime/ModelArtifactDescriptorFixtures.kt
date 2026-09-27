package mihon.model.artifacts.runtime

import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.descriptor.ModelArtifactLicense
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.security.MessageDigest

internal fun artifactFile(name: String, content: ByteArray): ModelArtifactFile = ModelArtifactFile(
    name = name,
    url = "https://models.example/$name",
    sizeBytes = content.size.toLong(),
    sha256 = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) },
)

internal fun artifactDescriptor(
    vararg files: ModelArtifactFile,
    id: String = "example.model",
    revision: String = "r1",
): ModelArtifactDescriptor = ModelArtifactDescriptor(
    id = ModelArtifactId(id),
    revision = revision,
    displayName = "Example model",
    files = files.toList(),
    license = ModelArtifactLicense("Apache-2.0", "https://license.example"),
    hosting = ModelArtifactHosting.Upstream("https://models.example"),
)

/** Routes the HTTPS artifact URLs declared by fixtures to a local plain-HTTP test server. */
internal fun MockWebServer.artifactHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val original = chain.request().url
        val local = url(original.encodedPath).toString().toHttpUrl()
        chain.proceed(chain.request().newBuilder().url(local).build())
    }
    .build()
