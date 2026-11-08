package app.katari.sync.protocol.account

/**
 * What a username and a password must be, wherever an account gets one: the server refuses anything else, and an app
 * checks the same rules before sending, so the person learns what is wrong before the server is asked.
 */
object AccountCredentials {
    const val MAX_USERNAME_LENGTH: Int = 64
    const val MIN_PASSWORD_LENGTH: Int = 8

    // Argon2 takes any length, but a password this long is a mistake or an attempt to make hashing costly.
    const val MAX_PASSWORD_LENGTH: Int = 1024

    /** Letters, digits, `.`, `_` and `-`, so a username is easy to type on every device. */
    fun isValidUsername(username: String): Boolean = username.length in 1..MAX_USERNAME_LENGTH &&
        username.all { it.isLetterOrDigit() || it in "._-" }

    fun isValidPassword(password: String): Boolean = password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH
}
