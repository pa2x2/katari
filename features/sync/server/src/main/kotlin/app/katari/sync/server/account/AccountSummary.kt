package app.katari.sync.server.account

data class AccountSummary(
    val id: Long,
    val username: String,
    val isAdmin: Boolean,
    val isDisabled: Boolean,
    /** Linked and not revoked. */
    val devices: Long,
    /** When one of its linked devices was last seen, or null when none has been. */
    val lastSeenMillis: Long?,
    /** Payloads and files kept for it, before compression. */
    val storedBytes: Long,
)
