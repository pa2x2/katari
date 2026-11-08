package app.katari.desktop.translation.server

import app.katari.translation.provider.server.ServerLocalNetworkGuard
import dev.zacsweers.metro.Inject
import okhttp3.OkHttpClient

/** Desktops have no permission for the local network, so a server client reaches it as it reaches any other. */
@Inject
class UnguardedServerLocalNetwork : ServerLocalNetworkGuard {
    override fun guard(client: OkHttpClient): OkHttpClient = client
}
