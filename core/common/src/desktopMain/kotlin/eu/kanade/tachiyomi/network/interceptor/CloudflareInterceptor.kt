package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import eu.kanade.tachiyomi.network.AndroidCookieJar
import mihon.core.common.web.AppChallengePage
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Android's Cloudflare interceptor, which extensions also build their own clients with: it solves challenges through
 * the shared solver in the app's challenge page ([AppChallengePage]), which, without JCEF, isn't offered, and a
 * challenge is then passed on unsolved and the user told the source needs a web view.
 */
class CloudflareInterceptor(
    context: Context,
    cookieManager: AndroidCookieJar,
    defaultUserAgentProvider: () -> String,
    messages: WebViewInterceptorMessages,
) : WebViewInterceptor(context, defaultUserAgentProvider, messages) {

    private val solver by lazy {
        CloudflareChallengeSolver(cookieManager, AppChallengePage.page, defaultUserAgentProvider, messages)
    }

    override fun prepareRequest(request: Request): Request {
        return request.withCloudflareRequestSnapshot()
    }

    override fun shouldIntercept(response: Response): Boolean {
        return response.isCloudflareChallenge()
    }

    override fun offersWebView(): Boolean = AppChallengePage.page.isOffered

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response = solver.solve(chain, request, response)
}
