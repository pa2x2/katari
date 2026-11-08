package app.katari.desktop.security.pam

import app.katari.desktop.security.signin.SignInConversation
import app.katari.desktop.security.signin.SignInOutcome
import app.katari.desktop.security.signin.SystemSignInCheck

/** Linux: the sign-in checked through PAM, with the system's shared sign-in service ([findPamService]). */
internal class PamSignInCheck : SystemSignInCheck {
    override fun configuration(): String? = if (pam == null) null else findPamService()

    override fun check(configuration: String, account: String, conversation: SignInConversation): SignInOutcome =
        pam?.let { authenticateWithPam(it, configuration, account, conversation) } ?: SignInOutcome.Failed
}
