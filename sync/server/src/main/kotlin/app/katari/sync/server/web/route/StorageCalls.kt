package app.katari.sync.server.web.route

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs a service call, which reads or writes the database and may hash a password, off the request threads. */
internal suspend fun <T> storage(block: () -> T): T = withContext(Dispatchers.IO) { block() }
