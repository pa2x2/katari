package app.katari.translation.provider.server

import app.katari.core.common.platform.PlatformFacility
import okhttp3.OkHttpClient

/**
 * Applies the platform's permission to reach the local network to a server client, so that a server on the local
 * network fails with the denial worded in the user's language while the permission is withheld.
 */
@PlatformFacility
fun interface ServerLocalNetworkGuard {
    fun guard(client: OkHttpClient): OkHttpClient
}
