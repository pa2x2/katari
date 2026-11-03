package eu.kanade.tachiyomi.network.tls

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

// A wrong pick fails every HTTPS request or silently trusts the JDK's store instead of the system's, on distributions
// the developer's machine isn't. The files hold no real certificates: only which ones are read is checked.
class LinuxCertificateAuthoritiesTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `each distribution's TLS bundle is read rather than the other trust files beside it`() {
        val layouts = mapOf(
            "debian" to Layout(
                files = listOf("etc/ssl/certs/ca-certificates.crt", "etc/ssl/certs/9d1dcd3c.0"),
                expected = "etc/ssl/certs/ca-certificates.crt",
            ),
            "fedora" to Layout(
                files = listOf(
                    "etc/pki/tls/certs/ca-bundle.trust.crt",
                    "etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem",
                    "etc/pki/ca-trust/extracted/pem/email-ca-bundle.pem",
                ),
                expected = "etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem",
            ),
            "opensuse" to Layout(
                files = listOf("etc/ssl/ca-bundle.pem", "etc/ssl/certs/9d1dcd3c.0"),
                expected = "etc/ssl/ca-bundle.pem",
            ),
            "alpine without ca-certificates" to Layout(
                files = listOf("etc/ssl/cert.pem"),
                expected = "etc/ssl/cert.pem",
            ),
            "an empty bundle left by a failed update" to Layout(
                files = listOf("etc/pki/tls/certs/ca-bundle.crt"),
                emptyFiles = listOf("etc/ssl/certs/ca-certificates.crt"),
                expected = "etc/pki/tls/certs/ca-bundle.crt",
            ),
            "nixos, through SSL_CERT_FILE" to Layout(
                files = listOf("etc/ssl/certs/ca-certificates.crt", "nix/store/abc-nss-cacert/ca-bundle.crt"),
                environment = mapOf("SSL_CERT_FILE" to "/nix/store/abc-nss-cacert/ca-bundle.crt"),
                expected = "nix/store/abc-nss-cacert/ca-bundle.crt",
            ),
        )
        layouts.forEach { (name, layout) ->
            val system = root.resolve(name)
            layout.files.forEach { system.write(it, "certificates") }
            layout.emptyFiles.forEach { system.write(it, "") }

            val files = LinuxCertificateAuthorities(layout.environment, system).certificateFiles()

            (name to files) shouldBe (name to listOf(system.resolve(layout.expected)))
        }
    }

    @Test
    fun `without a bundle the certificate files of the system's folder are read, and nothing when it has none`() {
        root.write("etc/ssl/certs/9d1dcd3c.0", "certificate")
        root.write("etc/ssl/certs/ISRG_Root_X1.pem", "certificate")
        root.write("etc/ssl/certs/README", "not a certificate")
        root.resolve("etc/ssl/certs/java").createDirectories()

        LinuxCertificateAuthorities(emptyMap(), root).certificateFiles() shouldBe listOf(
            root.resolve("etc/ssl/certs/9d1dcd3c.0"),
            root.resolve("etc/ssl/certs/ISRG_Root_X1.pem"),
        )
        val bare = Files.createDirectory(root.resolve("bare"))
        LinuxCertificateAuthorities(emptyMap(), bare).certificateFiles() shouldBe emptyList()
    }

    private class Layout(
        val files: List<String>,
        val expected: String,
        val emptyFiles: List<String> = emptyList(),
        val environment: Map<String, String> = emptyMap(),
    )

    private fun Path.write(path: String, text: String) {
        resolve(path).createParentDirectories().writeText(text)
    }
}
