package mihon.app.localnetwork

import android.content.Context
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

fun Context.localNetworkAccessDeniedMessage(host: String): String =
    stringResource(MR.strings.local_network_access_denied, host)
