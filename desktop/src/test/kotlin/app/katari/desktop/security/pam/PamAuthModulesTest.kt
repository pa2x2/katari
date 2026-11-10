package app.katari.desktop.security.pam

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class PamAuthModulesTest {

    // Where the shared stack reads the finger itself, the app must leave the reader to it, or pam_fprintd finds it
    // claimed. Distributions reach the fingerprint module through includes, bracketed controls and continued lines;
    // missing one of those makes the app claim the reader there.
    @Test
    fun `a fingerprint step reached through includes is found`(@TempDir root: Path) {
        val etc = root.resolve("etc").createDirectories()
        val vendor = root.resolve("usr").createDirectories()
        // Debian's common-auth with fingerprint login turned on, through @include
        etc.resolve("debian-login").writeText("@include common-auth\n")
        vendor.resolve("common-auth").writeText(
            """
            auth	[success=2 default=ignore]	pam_fprintd.so max-tries=1 timeout=10 # debian-tweak
            auth	[success=1 default=ignore]	pam_unix.so nullok
            """.trimIndent(),
        )
        // Fedora's system-auth through substack, with the module on a continued line and a full path
        etc.resolve("fedora-login").writeText("-auth substack system-auth\naccount include system-auth\n")
        etc.resolve("system-auth").writeText("auth sufficient \\\n  /usr/lib64/security/pam_fprintd.so\n")
        // Arch's login, whose fingerprint module is in a service of the desktop's, and only in a comment here
        etc.resolve("arch-login").writeText(
            """
            auth include system-local-login
            # auth sufficient pam_fprintd.so
            account required pam_fprintd.so
            """.trimIndent(),
        )
        etc.resolve("system-local-login").writeText("auth include arch-login\nauth required pam_unix.so\n")
        val directories = listOf(etc, vendor)

        pamAuthModules("debian-login", directories) shouldContain "pam_fprintd.so"
        pamAuthModules("fedora-login", directories) shouldContain "pam_fprintd.so"
        val arch = pamAuthModules("arch-login", directories)
        arch shouldContain "pam_unix.so"
        arch shouldNotContain "pam_fprintd.so"
    }
}
