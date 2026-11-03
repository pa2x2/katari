package eu.kanade.tachiyomi.source.aniyomi

import eu.kanade.tachiyomi.source.failure.runExtensionCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a call into the extension on the IO pool, with the errors [runExtensionCode] covers turned into exceptions.
 * Extension code blocks freely (OkHttp's `execute()`, `runBlocking`, latches, WebView extractors waiting on the main
 * looper), so no caller's thread may run it. Cancellation reaches it only at its own suspension points.
 */
internal suspend fun <T> callExtension(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO) { runExtensionCode { block() } }
