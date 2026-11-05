package app.katari.sync.server.account

/** What a username and a password must be, wherever an account gets one. */
object AccountCredentials {
    private const val MAX_USERNAME_LENGTH = 64
    private const val MIN_PASSWORD_LENGTH = 8

    // Argon2 takes any length, but a password this long is a mistake or an attempt to make hashing costly.
    private const val MAX_PASSWORD_LENGTH = 1024

    /** Letters, digits, `.`, `_` and `-`, so a username is easy to type on every device. */
    fun isValidUsername(username: String): Boolean = username.length in 1..MAX_USERNAME_LENGTH &&
        username.all { it.isLetterOrDigit() || it in "._-" }

    fun isValidPassword(password: String): Boolean = password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH
}
