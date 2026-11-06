package app.katari.sync.server.device

interface LinkCodeStore {
    fun insert(codeHash: ByteArray, accountId: Long, expiresAtMillis: Long)

    /** Removes the code and returns its account, or returns null when no unexpired code has [codeHash]. */
    fun take(codeHash: ByteArray, nowMillis: Long): Long?

    fun deleteExpired(nowMillis: Long)

    fun deleteForAccount(accountId: Long)
}
