package eu.kanade.tachiyomi.network.tls

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap

/**
 * Issuer certificates from the addresses certificates name for them, each address fetched once per run. They come over
 * plain HTTP, as RFC 5280 publishes them, and outside the app's clients, since they're fetched while one of those
 * clients is checking a server.
 */
internal class IssuerCertificateDownloads {
    private val downloaded = ConcurrentHashMap<String, List<X509Certificate>>()

    /** The certificates at [url], DER or PKCS #7, or none when it can't be fetched or read. */
    fun certificatesAt(url: String): List<X509Certificate> {
        downloaded[url]?.let { return it }
        val certificates = try {
            CertificateFactory.getInstance("X.509")
                .generateCertificates(download(url).inputStream())
                .filterIsInstance<X509Certificate>()
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't fetch the issuer certificate at $url" }
            return emptyList()
        } catch (e: CertificateException) {
            logcat(LogPriority.WARN, e) { "The issuer certificate at $url doesn't parse" }
            return emptyList()
        }
        return certificates.also { downloaded[url] = it }
    }

    private fun download(url: String): ByteArray {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.instanceFollowRedirects = false
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            return connection.inputStream.use { input ->
                val bytes = input.readNBytes(MAX_BYTES + 1)
                if (bytes.size > MAX_BYTES) throw IOException("Larger than $MAX_BYTES bytes")
                bytes
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000

        // A certificate is 1–2 KB, a PKCS #7 bundle of a few of them not much more
        const val MAX_BYTES = 64 * 1024
    }
}
