package mihon.extension.compat.webkit

import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import mihon.core.common.web.ExtensionPageRequest
import mihon.core.common.web.ExtensionPageResponse

/** A hidden page's request as Android's web view shows one to its client. Chromium tells of no redirect or gesture. */
internal class PageResourceRequest(private val request: ExtensionPageRequest) : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(request.url)

    override fun isForMainFrame(): Boolean = request.isForMainFrame

    override fun isRedirect(): Boolean = false

    override fun hasGesture(): Boolean = false

    override fun getMethod(): String = request.method

    override fun getRequestHeaders(): Map<String, String> = request.headers
}

internal class PageResourceError(private val errorCode: Int, private val description: String) : WebResourceError() {
    override fun getErrorCode(): Int = errorCode

    override fun getDescription(): CharSequence = description
}

internal class PageGoneDetail(private val crashed: Boolean) : RenderProcessGoneDetail() {
    override fun didCrash(): Boolean = crashed

    // The host doesn't rank its pages' renderers
    override fun rendererPriorityAtExit(): Int = WebView.RENDERER_PRIORITY_IMPORTANT
}

/** What a client's [WebResourceResponse] answers a hidden page's request with. */
internal fun WebResourceResponse.toPageResponse() = ExtensionPageResponse(
    statusCode = statusCode,
    reasonPhrase = reasonPhrase.orEmpty(),
    mimeType = mimeType,
    encoding = encoding,
    headers = responseHeaders.orEmpty(),
    body = data,
)
