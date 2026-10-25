package mihon.app.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

class LocalizedWebViewInterceptorMessages(private val context: Context) : WebViewInterceptorMessages {
    override fun webViewRequired(): String = context.stringResource(MR.strings.information_webview_required)

    override fun webViewOutdated(): String = context.stringResource(MR.strings.information_webview_outdated)

    override fun cloudflareBypassFailed(): String =
        context.stringResource(MR.strings.information_cloudflare_bypass_failure)
}
