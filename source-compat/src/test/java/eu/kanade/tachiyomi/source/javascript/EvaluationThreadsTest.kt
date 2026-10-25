package eu.kanade.tachiyomi.source.javascript

import app.cash.quickjs.QuickJs
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EvaluationThreadsTest {

    // The deobfuscator StreamWish-based extensions bundle recurses this deep. Evaluating on the caller's thread either
    // overflows at quickjs-kt's default 256 KiB limit or, with a raised limit, overruns a smaller caller stack and
    // kills the process.
    @Test
    fun `deep recursion succeeds from a caller with a small stack`() {
        var result: Result<Any?>? = null
        val caller = Thread(null, {
            result = runCatching {
                QuickJs.create().use {
                    it.evaluate("function depth(n) { return n == 0 ? 0 : 1 + depth(n - 1); } depth(800)")
                }
            }
        }, "small-stack caller", 256L shl 10)
        caller.start()
        caller.join()

        result!!.getOrThrow() shouldBe 800
    }
}
