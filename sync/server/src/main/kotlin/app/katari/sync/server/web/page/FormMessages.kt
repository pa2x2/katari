package app.katari.sync.server.web.page

import app.katari.sync.protocol.account.AccountCredentials
import kotlin.time.Duration

/** What forms say when their fields break a rule every account follows, worded once for every form. */
internal object FormMessages {
    val USERNAME_RULE = "Use up to ${AccountCredentials.MAX_USERNAME_LENGTH} letters, digits, dots, dashes or " +
        "underscores, without spaces."
    val PASSWORD_RULE = "Use at least ${AccountCredentials.MIN_PASSWORD_LENGTH} characters."
    const val PASSWORDS_DIFFER = "The two passwords are not the same."
    const val USERNAME_TAKEN = "Another account already has this username."
    const val WRONG_PASSWORD = "That password isn't right."

    fun tooManyAttempts(retryAfter: Duration): String {
        val minutes = retryAfter.inWholeMinutes + 1
        return "Too many failed attempts. Try again in ${plural(minutes, "minute")}."
    }
}
