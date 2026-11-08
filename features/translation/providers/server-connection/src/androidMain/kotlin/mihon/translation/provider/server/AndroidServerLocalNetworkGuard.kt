package mihon.translation.provider.server

import android.content.Context
import eu.kanade.tachiyomi.network.localnetwork.guardingLocalNetworkAccess
import okhttp3.OkHttpClient
import tachiyomi.i18n.MR
import tachiyomi.i18n.context.stringResource
import tachiyomi.i18n.local_network_access_denied

/** Guards a server client against Android's local network permission, wording a denial in the user's language. */
internal class AndroidServerLocalNetworkGuard(private val context: Context) : ServerLocalNetworkGuard {
    override fun guard(client: OkHttpClient): OkHttpClient = client.guardingLocalNetworkAccess(context) { host ->
        context.stringResource(MR.strings.local_network_access_denied, host)
    }
}
