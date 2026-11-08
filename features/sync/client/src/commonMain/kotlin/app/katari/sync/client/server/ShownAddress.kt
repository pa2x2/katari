package app.katari.sync.client.server

import okhttp3.HttpUrl

/**
 * The server's address as a person types it: without the slash a URL with no path ends in, which [ServerAddress.parse]
 * takes either way.
 */
val HttpUrl.shownAddress: String get() = toString().removeSuffix("/")
