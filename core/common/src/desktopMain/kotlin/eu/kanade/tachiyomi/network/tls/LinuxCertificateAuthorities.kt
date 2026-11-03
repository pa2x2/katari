package eu.kanade.tachiyomi.network.tls

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Path
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

/**
 * The certificate authorities of a Linux system: the PEM bundle its CA package extracts for TLS, which is where each
 * distribution keeps it, else a folder of single certificates. OpenSSL's `SSL_CERT_FILE` and `SSL_CERT_DIR` come first,
 * as they do for OpenSSL and Go programs; NixOS sets them for its store.
 *
 * @param root the file system root, another folder only in tests.
 */
class LinuxCertificateAuthorities(
    private val environment: Map<String, String> = System.getenv(),
    private val root: Path = Path("/"),
) : SystemCertificateAuthorities {

    override fun certificates(): List<X509Certificate> {
        val files = certificateFiles()
        val certificates = files.flatMap(::readPemCertificates)
        if (files.isNotEmpty() && certificates.isEmpty()) {
            logcat(LogPriority.WARN) { "No certificates in ${files.joinToString()}; trusting the JDK's store" }
        }
        return certificates
    }

    /** The files whose certificates are trusted: one bundle, else every certificate file of one folder. */
    internal fun certificateFiles(): List<Path> {
        val bundle = environment.absolutePath(CERT_FILE_VARIABLE)?.takeIf { it.isNonEmptyFile() }
            ?: BUNDLE_FILES.map(::system).firstOrNull { it.isNonEmptyFile() }
        if (bundle != null) return listOf(bundle)
        val folder = environment.absolutePath(CERT_DIR_VARIABLE)?.takeIf { it.isDirectory() }
            ?: CERTIFICATE_FOLDERS.map(::system).firstOrNull { it.isDirectory() }
            ?: return emptyList()
        return folder.listDirectoryEntries()
            .filter { CERTIFICATE_FILE_NAME.matches(it.fileName.toString()) && it.isNonEmptyFile() }
            .sorted()
    }

    private fun system(path: String): Path = root.resolve(path.removePrefix("/"))

    private fun Map<String, String>.absolutePath(variable: String): Path? =
        get(variable)?.takeIf { it.isNotEmpty() }?.let(::Path)?.takeIf { it.isAbsolute }?.let { system(it.toString()) }

    private fun Path.isNonEmptyFile(): Boolean = isRegularFile() && fileSize() > 0

    /** Every certificate in [file], skipping the ones that don't parse, as OpenSSL does when it loads a bundle. */
    private fun readPemCertificates(file: Path): List<X509Certificate> {
        val text = try {
            file.readText(Charsets.US_ASCII)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't read the certificates in $file" }
            return emptyList()
        }
        val factory = CertificateFactory.getInstance("X.509")
        return PEM_CERTIFICATE.findAll(text).mapNotNull { match ->
            try {
                val der = Base64.getMimeDecoder().decode(match.groupValues[1])
                factory.generateCertificate(der.inputStream()) as X509Certificate
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: CertificateException) {
                null
            }
        }.toList()
    }

    private companion object {
        const val CERT_FILE_VARIABLE = "SSL_CERT_FILE"
        const val CERT_DIR_VARIABLE = "SSL_CERT_DIR"

        // The bundles extracted for TLS only. Fedora's ca-bundle.trust.crt beside them lists every purpose's
        // authorities in OpenSSL's "TRUSTED CERTIFICATE" form, which only OpenSSL reads.
        val BUNDLE_FILES = listOf(
            // Debian, Ubuntu, Arch, Gentoo, Alpine, Void, NixOS
            "/etc/ssl/certs/ca-certificates.crt",
            // Fedora, RHEL, CentOS
            "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem",
            "/etc/pki/tls/certs/ca-bundle.crt",
            // openSUSE, SLES
            "/etc/ssl/ca-bundle.pem",
            "/var/lib/ca-certificates/ca-bundle.pem",
            // Arch's own name for the bundle the first entry links to
            "/etc/ca-certificates/extracted/tls-ca-bundle.pem",
            // Alpine's minimal bundle without the ca-certificates package
            "/etc/ssl/cert.pem",
        )

        val CERTIFICATE_FOLDERS = listOf("/etc/ssl/certs", "/etc/pki/tls/certs")

        // A folder of certificates holds them as *.pem or *.crt, or as OpenSSL's hash links such as 9d1dcd3c.0
        val CERTIFICATE_FILE_NAME = Regex(".+\\.(pem|crt)|[0-9a-f]{8}\\.\\d+")

        val PEM_CERTIFICATE = Regex("-----BEGIN CERTIFICATE-----([A-Za-z0-9+/=\\s]+?)-----END CERTIFICATE-----")
    }
}
