package app.katari.webview.jcef.page.extension

import app.katari.core.common.web.ExtensionPageResponse
import logcat.LogPriority
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.io.InputStream

/**
 * Answers a page's request with [response] instead of the network: its status, headers and type, and its body as
 * Chromium reads it. JBR's Chromium server reads a handler's answer through the synchronous calls only.
 */
internal class InterceptedResponse(private val response: ExtensionPageResponse) : CefResourceHandlerAdapter() {
    private val body: InputStream? = response.body

    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        callback.Continue()
        return true
    }

    override fun getResponseHeaders(cefResponse: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        cefResponse.status = response.statusCode
        cefResponse.statusText = response.reasonPhrase
        response.mimeType?.let { cefResponse.mimeType = it }
        response.headers.forEach { (name, value) -> cefResponse.setHeaderByName(name, value, true) }
        val mimeType = response.mimeType
        val encoding = response.encoding
        if (mimeType != null && encoding != null) {
            cefResponse.setHeaderByName("Content-Type", "$mimeType; charset=$encoding", true)
        }
        // The length isn't known before the body has been read
        responseLength.set(if (body == null) 0 else UNKNOWN_LENGTH)
    }

    override fun readResponse(dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        val body = body ?: return false
        val read = try {
            body.read(dataOut, 0, bytesToRead)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "An extension's answer to a page's request couldn't be read" }
            END_OF_BODY
        }
        if (read == END_OF_BODY) {
            close()
            bytesRead.set(0)
            return false
        }
        bytesRead.set(read)
        return true
    }

    override fun cancel() {
        close()
    }

    private fun close() {
        try {
            body?.close()
        } catch (_: IOException) {
            // Nothing more is read from it
        }
    }

    private companion object {
        const val UNKNOWN_LENGTH = -1
        const val END_OF_BODY = -1
    }
}

/** Refuses a page's request, which then fails as one that was blocked. */
internal class RefusedRequest : CefResourceHandlerAdapter() {
    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean = false
}
