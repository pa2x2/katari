package eu.kanade.tachiyomi.network.tls

import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

/**
 * [platform], which a server's chain fails when its root is one the system doesn't have, retried with the issuers the
 * chain's top certificate names in its Authority Information Access, as browsers do. A newer root that the system's
 * store doesn't have yet but Android's does (Let's Encrypt's "Root YE", say) is published there cross-signed by an older
 * root the system trusts, so the completed chain ends at a trusted root. Nothing fetched is trusted by itself: the
 * completed chain must still lead to one of [platform]'s authorities.
 */
internal class IssuerCompletingTrustManager(
    private val platform: X509ExtendedTrustManager,
    private val issuers: IssuerCertificateDownloads,
) : X509ExtendedTrustManager() {

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) =
        checkCompleting(chain) { platform.checkServerTrusted(it, authType, socket) }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) =
        checkCompleting(chain) { platform.checkServerTrusted(it, authType, engine) }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) =
        checkCompleting(chain) { platform.checkServerTrusted(it, authType) }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) =
        platform.checkClientTrusted(chain, authType, socket)

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) =
        platform.checkClientTrusted(chain, authType, engine)

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        platform.checkClientTrusted(chain, authType)

    override fun getAcceptedIssuers(): Array<X509Certificate> = platform.acceptedIssuers

    private fun checkCompleting(chain: Array<X509Certificate>, check: (Array<X509Certificate>) -> Unit) {
        val failure = try {
            check(chain)
            return
        } catch (e: CertificateException) {
            e
        }
        var completed = chain
        repeat(MAX_FETCHED_ISSUERS) {
            val top = completed.lastOrNull() ?: throw failure
            if (top.issuerX500Principal == top.subjectX500Principal) throw failure
            val found = top.caIssuerUrls()
                .flatMap(issuers::certificatesAt)
                .filter { it.subjectX500Principal == top.issuerX500Principal && it !in completed }
            if (found.isEmpty()) throw failure
            completed += found
            try {
                check(completed)
                return
            } catch (e: CertificateException) {
                failure.addSuppressed(e)
            }
        }
        throw failure
    }

    private companion object {
        // Cross-signs are one level deep; a few more cover an intermediate a server leaves out above it
        const val MAX_FETCHED_ISSUERS = 3
    }
}
