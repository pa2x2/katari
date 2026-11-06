package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceId

interface LinkCodeStore {
    fun insert(codeHash: ByteArray, accountId: Long, issuedBy: DeviceId?, expiresAtMillis: Long)

    /** The unexpired code with [codeHash], leaving it in place; null when there is none. */
    fun unexpired(codeHash: ByteArray, nowMillis: Long): IssuedLinkCode?

    /** Removes the code and returns its account, or returns null when no unexpired code has [codeHash]. */
    fun take(codeHash: ByteArray, nowMillis: Long): Long?

    fun deleteExpired(nowMillis: Long)

    fun deleteForAccount(accountId: Long)
}

/** A link code's account, and the device that showed it ([issuedBy]), null when no device asked for it. */
data class IssuedLinkCode(val accountId: Long, val issuedBy: DeviceId?)
