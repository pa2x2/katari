package app.katari.sync.server.websession

interface WebSessionStore {
    fun insert(tokenHash: ByteArray, accountId: Long, expiresAtMillis: Long)

    /** The account of the unexpired session with [tokenHash], or null when there is none. */
    fun accountOf(tokenHash: ByteArray, nowMillis: Long): Long?

    fun delete(tokenHash: ByteArray)

    fun deleteForAccount(accountId: Long)

    fun deleteExpired(nowMillis: Long)
}
