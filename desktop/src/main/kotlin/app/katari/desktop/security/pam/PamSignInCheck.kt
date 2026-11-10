package app.katari.desktop.security.pam

import app.katari.desktop.security.fprintd.FprintdFingerprintReader
import app.katari.desktop.security.signin.FingerprintReader
import app.katari.desktop.security.signin.SignInConversation
import app.katari.desktop.security.signin.SignInOutcome
import app.katari.desktop.security.signin.SystemSignInCheck

/** Linux: the sign-in checked through PAM, with the system's shared sign-in service ([findPamService]). */
internal class PamSignInCheck : SystemSignInCheck {
    override fun configuration(): String? = if (pam == null) null else findPamService()

    override fun check(configuration: String, account: String, conversation: SignInConversation): SignInOutcome =
        pam?.let { authenticateWithPam(it, configuration, account, conversation) } ?: SignInOutcome.Failed

    /**
     * fprintd's reader, unless the service reads the finger itself, as Debian's and Ubuntu's `common-auth` do with
     * fingerprint login turned on: the reader takes one client at a time, so the PAM check would find it busy.
     * Desktops more often keep the fingerprint out of the shared stack and read it in a service of their own beside
     * the password, as Plasma's lock screen does with `kde-fingerprint`.
     */
    override fun fingerprintReader(configuration: String): FingerprintReader? =
        if (FPRINTD_MODULE in pamAuthModules(configuration)) null else FprintdFingerprintReader()

    private companion object {
        const val FPRINTD_MODULE = "pam_fprintd.so"
    }
}
