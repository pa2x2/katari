package mihon.translation.provider.server

import android.content.Context
import eu.kanade.tachiyomi.network.localnetwork.guardingLocalNetworkAccess
import okhttp3.OkHttpClient
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** Guards a server client against Android's local network permission, wording a denial in the user's language. */
fun OkHttpClient.guardingServerLocalNetworkAccess(context: Context): OkHttpClient {
    return guardingLocalNetworkAccess(context) { host ->
        context.stringResource(MR.strings.local_network_access_denied, host)
    }
}
