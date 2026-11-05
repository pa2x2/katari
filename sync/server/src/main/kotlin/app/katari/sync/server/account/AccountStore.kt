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
}
