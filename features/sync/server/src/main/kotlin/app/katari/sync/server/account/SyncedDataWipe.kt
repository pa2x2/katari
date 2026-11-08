package app.katari.sync.server.account

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.server.file.FileContentStore
import app.katari.sync.server.live.AccountChanges
import app.katari.sync.server.record.HistoryChain
import app.katari.sync.server.record.chainAt
import app.katari.sync.server.storage.ServerStorage
import kotlin.time.Duration

/**
 * "Delete my synced data": removes every record, file and pending link code the account holds on the server and
 * revokes all its devices, which carry on standalone with everything they hold. The account stays, so it can start
 * again from an empty server copy.
 *
 * The account's history starts over one sequence past where it was ([HistoryChain.restart]), and nothing before that
 * is kept, so any position a device held before is refused as expired, and a device that joins later takes the empty
 * account the usual way. Sequences are never reused, so no old position can match the new history by accident.
 */
class SyncedDataWipe(
    private val storage: ServerStorage,
    private val passwordCheck: PasswordCheck,
    private val content: FileContentStore,
    private val changes: AccountChanges,
) {
    fun wipe(account: Account, password: String, remoteAddress: String): WipeOutcome {
        when (val check = passwordCheck.check(account.username, password, remoteAddress)) {
            is PasswordCheckOutcome.Matched -> if (check.account.id != account.id) return WipeOutcome.WrongPassword
            PasswordCheckOutcome.Wrong -> return WipeOutcome.WrongPassword
            is PasswordCheckOutcome.Throttled -> return WipeOutcome.Throttled(check.retryAfter)
        }
        val revoked: List<DeviceId> = storage.transaction {
            val devices = storage.devices.activeIn(account.id).map { it.id }
            storage.devices.revokeAll(account.id)
            storage.linkCodes.deleteForAccount(account.id)
            val sequence = storage.records.sequence(account.id)
            val restart = sequence + 1
            val previous = checkNotNull(storage.chainAt(account.id, sequence))
            storage.records.clear(account.id, restart, HistoryChain.restart(previous, restart))
            storage.files.removeAccount(account.id)
            devices
        }
        content.deleteAccount(account.id)
        revoked.forEach(changes::announceRevoked)
        return WipeOutcome.Wiped
    }
}

sealed interface WipeOutcome {
    data object Wiped : WipeOutcome

    data object WrongPassword : WipeOutcome

    data class Throttled(val retryAfter: Duration) : WipeOutcome
}
