package eu.kanade.tachiyomi.source.aniyomi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a call into the extension on the IO pool. Extension code blocks freely (OkHttp's `execute()`, `runBlocking`,
 * latches, WebView extractors waiting on the main looper), so no caller's thread may run it. Cancellation reaches it
 * only at its own suspension points.
 */
internal suspend fun <T> callExtension(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)
