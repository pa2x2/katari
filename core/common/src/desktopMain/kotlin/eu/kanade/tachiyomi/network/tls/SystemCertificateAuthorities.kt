package eu.kanade.tachiyomi.network.tls

import java.security.cert.X509Certificate

/**
 * The certificate authorities the operating system trusts for TLS. Android's network trusts its system's store; on
 * desktop the equivalent is the system's store too, not the JDK's own `cacerts`, which a runtime shipped with the app
 * brings from wherever it was built and which the system's updates never reach.
 */
fun interface SystemCertificateAuthorities {
    /** Empty when the system has no store this implementation can read, in which case the JDK's store is trusted. */
    fun certificates(): List<X509Certificate>
}
