package app.katari.sync.server.invite

interface InviteStore {
    fun insert(codeHash: ByteArray, expiresAtMillis: Long)

    fun isUnexpired(codeHash: ByteArray, nowMillis: Long): Boolean

    /** Removes the invite and returns whether there was an unexpired one with [codeHash]. */
    fun take(codeHash: ByteArray, nowMillis: Long): Boolean

    fun deleteExpired(nowMillis: Long)
}
