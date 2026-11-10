package app.katari.desktop.security.signin

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.security.pam.PamSignInCheck

/**
 * How the operating system checks the user's sign-in. The app finds the [configuration] the system checks with, and
 * the sign-in check process ([SignInCheckProcess]) started with it runs the [check].
 */
interface SystemSignInCheck {
    /** What the check runs with, passed to the check process as text; null where the system can't check. */
    fun configuration(): String?

    /**
     * Checks [account]'s sign-in with [configuration], with what the system says and asks going through
     * [conversation]. Blocks until the system answers.
     */
    fun check(configuration: String, account: String, conversation: SignInConversation): SignInOutcome

    /**
     * The reader the app reads the user's finger on beside [check] with [configuration]; null where there's none, or
     * where [check] reads the finger itself.
     */
    fun fingerprintReader(configuration: String): FingerprintReader?
}

/** Where the system's messages go and its questions get their answers, one at a time, as the check runs. */
interface SignInConversation {
    fun tell(message: SignInCheckEvent.Message)

    fun ask(question: SignInCheckEvent.Question): String
}

fun systemSignInCheck(operatingSystem: DesktopOperatingSystem): SystemSignInCheck = when (operatingSystem) {
    DesktopOperatingSystem.LINUX -> PamSignInCheck()
}
