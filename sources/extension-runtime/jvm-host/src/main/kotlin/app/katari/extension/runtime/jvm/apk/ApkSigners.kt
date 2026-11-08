package app.katari.extension.runtime.jvm.apk

import com.android.apksig.ApkVerifier
import eu.kanade.tachiyomi.util.lang.Hash
import java.io.File

/**
 * The signers of an apk, checked against its contents with every signature scheme Android checks (v1 to v4), and given
 * in the order Android's `SigningInfo` gives them: with several signers, each signer of the contents; with one, its
 * certificate lineage oldest first, so the last is the certificate the apk is signed with now.
 */
internal object ApkSigners {

    /** The SHA-256 of each signer's certificate, `null` when [apk]'s signatures don't verify, or it has none. */
    fun of(apk: File): List<String>? {
        val result = ApkVerifier.Builder(apk).build().verify()
        if (!result.isVerified) return null
        val signers = result.signerCertificates
        val certificates = if (signers.size > 1) {
            signers
        } else {
            result.signingCertificateLineage?.certificatesInLineage?.takeIf { it.isNotEmpty() } ?: signers
        }
        return certificates.map { Hash.sha256(it.encoded) }
    }
}
