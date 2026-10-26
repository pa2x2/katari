package mihon.script.runtime

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

class ScriptRuntimePoolTest {

    // A plugin reads its settings while it is constructed, so a runtime loaded before a settings change or an update
    // must not serve a call after it, even when it was busy at the time of the change
    @Test
    fun `a runtime busy during invalidate is not reused afterwards`() = runBlocking<Unit> {
        val pool = ScriptRuntimePool(ScriptRuntimeFactory())
        var version = 1
        val spec = {
            ScriptRuntimeSpec("versioned", listOf(ScriptSource("v$version.js", "function v() { return $version; }")))
        }
        val inside = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        val busy = async {
            pool.use("plugin", spec) { runtime ->
                inside.complete(Unit)
                release.await()
                runtime.call("v", JsonArray(emptyList()))
            }
        }
        inside.await()
        version = 2
        pool.invalidate { it == "plugin" }
        release.complete(Unit)

        busy.await() shouldBe JsonPrimitive(1)
        pool.use("plugin", spec) { it.call("v", JsonArray(emptyList())) } shouldBe JsonPrimitive(2)
    }
}
