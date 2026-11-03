package mihon.tools.extensioncompatibility.classification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedTrustManager
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration

/**
 * The certificate a site answers with when it is its own issuer, as a server's default certificate is (often one for
 * `example.com`, from a proxy in front of the site): no store trusts it, Android's neither, so a source failing on it
 * is the site's error rather than a root desktop lacks. Each host is asked once per run.
 */
internal class SelfSignedCertificateProbe(private val timeout: Duration) {
    private val answers = ConcurrentHashMap<String, Optional<String>>()

    /** The self-signed certificate's subject, or null when the site's certificate has another issuer. */
    suspend fun selfSignedSubjectOf(url: String): String? {
        val parsed = url.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: return null
        answers[parsed.host]?.let { return it.getOrNull() }
        val subject = withContext(Dispatchers.IO) {
            try {
                serverCertificate(parsed.host, parsed.port)
                    ?.takeIf { it.issuerX500Principal == it.subjectX500Principal }
                    ?.subjectX500Principal?.name
            } catch (_: IOException) {
                null
            }
        }
        answers[parsed.host] = Optional.ofNullable(subject)
        return subject
    }

    private fun serverCertificate(host: String, port: Int): X509Certificate? {
        val recorder = ChainRecorder()
        val tls = SSLContext.getInstance("TLS").apply { init(null, arrayOf(recorder), null) }
        Socket().use { plain ->
            plain.connect(InetSocketAddress(host, port), timeout.inWholeMilliseconds.toInt())
            plain.soTimeout = timeout.inWholeMilliseconds.toInt()
            (tls.socketFactory.createSocket(plain, host, port, false) as SSLSocket).use { socket ->
                socket.sslParameters = socket.sslParameters.apply { serverNames = listOf(SNIHostName(host)) }
                socket.startHandshake()
            }
        }
        return recorder.chain?.firstOrNull()
    }

    /** Accepts any server, only to see what it presents; the connection carries nothing. */
    private class ChainRecorder : X509ExtendedTrustManager() {
        @Volatile
        var chain: Array<X509Certificate>? = null

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) {
            this.chain = chain
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) {
            this.chain = chain
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            this.chain = chain
        }

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) = Unit

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) = Unit

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
