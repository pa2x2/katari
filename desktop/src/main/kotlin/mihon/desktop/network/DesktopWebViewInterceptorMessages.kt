package mihon.desktop.network

import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource

/**
 * What the network's web view interceptors tell the user on desktop, which has no web view yet: a response only a web
 * view gets through is passed on, and the source is said to need one. The other two are Android's, for a web view
 * desktop doesn't reach yet.
 */
class DesktopWebViewInterceptorMessages : WebViewInterceptorMessages {
    override fun webViewRequired(): String =
        stringResource(MR.strings.platform_gap_source_message, stringResource(MR.strings.platform_gap_web_view))

    override fun webViewOutdated(): String = stringResource(MR.strings.information_webview_outdated)

    override fun cloudflareBypassFailed(): String = stringResource(MR.strings.information_cloudflare_bypass_failure)
}
