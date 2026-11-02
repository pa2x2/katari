package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import eu.kanade.tachiyomi.network.AndroidCookieJar
import mihon.core.common.web.AndroidChallengePage
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/** Solves Cloudflare's challenges in an off-screen WebView ([AndroidChallengePage]) through the shared solver. */
class CloudflareInterceptor(
    context: Context,
    cookieManager: AndroidCookieJar,
    defaultUserAgentProvider: () -> String,
    messages: WebViewInterceptorMessages,
) : WebViewInterceptor(context, defaultUserAgentProvider, messages) {

    private val page = AndroidChallengePage(context, messages)
    private val solver = CloudflareChallengeSolver(cookieManager, page, defaultUserAgentProvider, messages)

    override fun prepareRequest(request: Request): Request {
        return request.withCloudflareRequestSnapshot()
    }

    override fun shouldIntercept(response: Response): Boolean {
        return response.isCloudflareChallenge()
    }

    override fun offersWebView(): Boolean = page.isOffered

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response = solver.solve(chain, request, response)
}
