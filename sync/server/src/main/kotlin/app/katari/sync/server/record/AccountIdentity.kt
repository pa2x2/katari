package app.katari.sync.server.record

import app.katari.sync.protocol.server.ServerIdentity
import app.katari.sync.server.storage.ServerStorage

internal fun ServerStorage.identityOf(accountId: Long): ServerIdentity =
    ServerIdentity(instanceId = instanceId, sequence = records.sequence(accountId))
