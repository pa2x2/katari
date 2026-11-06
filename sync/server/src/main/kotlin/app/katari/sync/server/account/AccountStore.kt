package app.katari.sync.server.account

interface AccountStore {
    fun count(): Long

    fun create(username: String, passwordHash: String, isAdmin: Boolean, createdAtMillis: Long): Account

    /** Usernames match without regard to ASCII case. */
    fun byUsername(username: String): Account?

    fun byId(id: Long): Account?

    fun ids(): List<Long>

    fun setPasswordHash(id: Long, passwordHash: String)

    fun setDisabled(id: Long, disabled: Boolean)

    fun setAdmin(id: Long, admin: Boolean)

    fun countEnabledAdmins(): Long

    /** Removes the account with everything it holds: devices, records, file index, link codes and web sessions. */
    fun delete(id: Long)

    /** Every account, by username. */
    fun summaries(): List<AccountSummary>

    fun summary(id: Long): AccountSummary?
}
