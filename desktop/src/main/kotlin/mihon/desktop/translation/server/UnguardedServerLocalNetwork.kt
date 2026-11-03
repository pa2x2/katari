package mihon.desktop.translation.server

import dev.zacsweers.metro.Inject
import mihon.translation.provider.server.ServerLocalNetworkGuard
import okhttp3.OkHttpClient

/** Desktops have no permission for the local network, so a server client reaches it as it reaches any other. */
@Inject
class UnguardedServerLocalNetwork : ServerLocalNetworkGuard {
    override fun guard(client: OkHttpClient): OkHttpClient = client
}
