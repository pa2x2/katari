package mihon.script.runtime.host

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import mihon.script.runtime.ScriptException
import mihon.script.runtime.ScriptLimits
import mihon.script.runtime.ScriptNetwork
import mihon.script.runtime.ScriptRuntimeFactory
import mihon.script.runtime.ScriptRuntimeSpec
import mihon.script.runtime.ScriptSource
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class QuickJsScriptRuntimeTest {

    private val noArgs = JsonArray(emptyList())

    // quickjs-kt waits forever, without using CPU, for a result promise nothing can settle any more (LNReader's
    // WuxiaWorld fetchProto throws inside a FileReader callback and leaves its promise pending)
    @Test
    fun `a call waiting on a promise nothing settles is stopped and closes its runtime`() = runBlocking<Unit> {
        val runtime = ScriptRuntimeFactory(ScriptNetwork(OkHttpClient()) { "test" }).open(
            ScriptRuntimeSpec(
                name = "stall",
                scripts = listOf(ScriptSource("stall.js", "function hang() { return new Promise(function () {}); }")),
                limits = ScriptLimits(stallTimeout = 1.seconds),
            ),
        )

        withTimeout(10.seconds) {
            shouldThrow<ScriptException.Stalled> { runtime.call("hang", noArgs) }
        }
        runtime.isOpen shouldBe false
    }

    // quickjs-kt returns only when every async binding of the evaluation has finished, so a request a script fires
    // and never awaits would hold the call's result until it finished
    @Test
    fun `host work still running when the result is ready doesn't hold the result`() = runBlocking<Unit> {
        val runtime = ScriptRuntimeFactory(ScriptNetwork(OkHttpClient()) { "test" }).open(
            ScriptRuntimeSpec(
                name = "pending",
                scripts = listOf(
                    ScriptSource(
                        "pending.js",
                        "function fire() { slow().then(function () { throw 1; }); return 'now'; }",
                    ),
                ),
                bindings = {
                    asyncFunction("slow") {
                        delay(60_000)
                        "late"
                    }
                },
            ),
        )

        withTimeout(10.seconds) {
            runtime.call("fire", noArgs) shouldBe JsonPrimitive("now")
            runtime.call("fire", noArgs) shouldBe JsonPrimitive("now")
        }
        runtime.close()
    }
}
