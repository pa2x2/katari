package app.katari.desktop.network

import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource

/**
 * What the network's web view interceptors tell the user on desktop. Where a response needs a web view desktop can't
 * offer, without JCEF or in an extension's own interceptor, it is passed on and the source is said to need one, or to
 * need the web view set up where that is what it [awaits][awaitsSetup]. Desktop
 * solves challenges in its own Chromium, never in an outdated WebView, so that message is Android's alone.
 */
class DesktopWebViewInterceptorMessages(private val awaitsSetup: () -> Boolean) : WebViewInterceptorMessages {
    override fun webViewRequired(): String = if (awaitsSetup()) {
        stringResource(MR.strings.webview_setup_needed_by_source)
    } else {
        stringResource(MR.strings.platform_gap_source_message, stringResource(MR.strings.platform_gap_web_view))
    }

    override fun webViewOutdated(): String = stringResource(MR.strings.information_webview_outdated)

    override fun cloudflareBypassFailed(): String = stringResource(MR.strings.information_cloudflare_bypass_failure)
}
