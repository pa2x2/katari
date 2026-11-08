package app.katari.gradle.download

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat

/**
 * Downloads [url] to [target] and checks it against the digest `gradle/katari.versions.toml` pins for it, [expected] in
 * [algorithm]. A download that doesn't match is deleted. [name] names the download in the build's messages.
 */
internal fun downloadPinned(
    name: String,
    url: String,
    target: File,
    algorithm: String,
    expected: String,
    logger: Logger,
) {
    logger.lifecycle("Downloading $name from $url")
    val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
        .build()
    val response = client.send(HttpRequest.newBuilder(URI(url)).build(), HttpResponse.BodyHandlers.ofInputStream())
    val digest = MessageDigest.getInstance(algorithm)
    response.body().use { body ->
        if (response.statusCode() != HTTP_OK) {
            throw GradleException("Can't download $name from $url: HTTP ${response.statusCode()}")
        }
        Files.copy(DigestInputStream(body, digest), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    val actual = HexFormat.of().formatHex(digest.digest())
    if (!actual.equals(expected, ignoreCase = true)) {
        target.delete()
        throw GradleException(
            "$name from $url has the $algorithm sum $actual, not the $expected that " +
                "gradle/katari.versions.toml pins. The download was deleted; it was damaged or replaced on the way, " +
                "or the pin is wrong.",
        )
    }
}

private const val CONNECT_TIMEOUT_SECONDS = 30L
private const val HTTP_OK = 200
