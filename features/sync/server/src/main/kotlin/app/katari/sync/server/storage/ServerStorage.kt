package app.katari.sync.server.storage

import app.katari.sync.server.account.AccountStore
import app.katari.sync.server.device.DeviceStore
import app.katari.sync.server.device.LinkCodeStore
import app.katari.sync.server.file.StoredFileIndex
import app.katari.sync.server.invite.InviteStore
import app.katari.sync.server.record.RecordStore
import app.katari.sync.server.websession.WebSessionStore

/**
 * Everything the server keeps apart from file contents, behind one seam so a database other than SQLite can be added
 * without touching the services. The services own every rule; stores only read and write.
 */
interface ServerStorage : AutoCloseable {
    /**
     * Picked at random when this storage was created and kept for as long as it lives, which is what lets a device
     * tell a server that started over from the one it synced with ([app.katari.sync.protocol.server.ServerIdentity]).
     */
    val instanceId: String

    val accounts: AccountStore
    val devices: DeviceStore
    val linkCodes: LinkCodeStore
    val records: RecordStore
    val files: StoredFileIndex
    val invites: InviteStore
    val webSessions: WebSessionStore

    /**
     * Runs [block] as one atomic unit that no other transaction interleaves with; store calls made inside it join it.
     * [block] runs on the calling thread and must not suspend or hand work to other threads.
     */
    fun <T> transaction(block: () -> T): T
}
