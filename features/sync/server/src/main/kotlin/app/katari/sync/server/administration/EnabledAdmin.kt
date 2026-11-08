package app.katari.sync.server.administration

import app.katari.sync.server.account.Account
import app.katari.sync.server.storage.ServerStorage

/**
 * The account with [id] when it may act as an admin now. Read from storage on every action rather than trusted from
 * the session that asked, so an admin who was demoted or disabled a moment ago can do nothing more.
 */
internal fun ServerStorage.enabledAdmin(id: Long): Account? =
    accounts.byId(id)?.takeIf { it.isAdmin && !it.isDisabled }
