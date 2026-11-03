package mihon.script.runtime.engine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * The threads every QuickJS in the app runs on. quickjs-kt recurses deeper than the scripts it runs were written for
 * (the deobfuscator bundled by StreamWish-based extensions overflows its default 256 KiB on moderately nested scripts,
 * cheerio on deeply nested pages), so every engine gets [MAX_STACK_SIZE]. QuickJS trusts that limit: one above the
 * thread's real stack overflows the native stack and kills the process, so the threads get a stack well above it.
 */
object EvaluationThreads {
    const val MAX_STACK_SIZE = 1L shl 20

    private const val THREAD_STACK_SIZE = 8L shl 20

    private val threadCount = AtomicInteger()

    val dispatcher: CoroutineDispatcher = Executors.newCachedThreadPool { runnable ->
        Thread(null, runnable, "QuickJs-${threadCount.incrementAndGet()}", THREAD_STACK_SIZE).apply { isDaemon = true }
    }.asCoroutineDispatcher()
}
