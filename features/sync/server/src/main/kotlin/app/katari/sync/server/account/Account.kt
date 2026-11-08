package app.katari.sync.server.account

data class Account(
    val id: Long,
    val username: String,
    /** In the form [PasswordHasher.hash] writes. */
    val passwordHash: String,
    val isAdmin: Boolean,
    val isDisabled: Boolean,
)
