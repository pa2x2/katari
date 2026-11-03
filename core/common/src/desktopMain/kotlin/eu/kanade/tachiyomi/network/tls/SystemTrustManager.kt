package eu.kanade.tachiyomi.network.tls

import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager

/**
 * The trust the app's HTTP clients check servers with on desktop: [authorities], or the JDK's store when the system has
 * none, completed by [IssuerCompletingTrustManager] so that chains to a root the system lacks but Android has still
 * reach one the system trusts.
 */
fun systemTrustManager(authorities: SystemCertificateAuthorities): X509ExtendedTrustManager {
    val certificates = authorities.certificates()
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(certificates.takeIf { it.isNotEmpty() }?.let(::keyStoreOf))
    val platform = factory.trustManagers.filterIsInstance<X509ExtendedTrustManager>().single()
    return IssuerCompletingTrustManager(platform, IssuerCertificateDownloads())
}

private fun keyStoreOf(certificates: List<X509Certificate>): KeyStore =
    KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        certificates.forEachIndexed { index, certificate -> setCertificateEntry("system-$index", certificate) }
    }
