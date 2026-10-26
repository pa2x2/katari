package mihon.script.runtime.host

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import mihon.script.runtime.ScriptException
import mihon.script.runtime.ScriptNetwork
import mihon.script.runtime.ScriptRuntimeFactory
import mihon.script.runtime.ScriptRuntimeSpec
import mihon.script.runtime.ScriptSource
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.SourcePreferenceProfile
import kotlin.time.Duration.Companion.seconds

class QuickJsScriptRuntimeTest {

    private val noArgs = JsonArray(emptyList())

    // quickjs-kt used to wait forever, without using CPU, for a result promise nothing can settle any more (LNReader's
    // WuxiaWorld fetchProto throws inside a FileReader callback and leaves its promise pending); now it fails at once,
    // well inside the stall limit
    @Test
    fun `a call waiting on a promise nothing settles is stopped at once and closes its runtime`() = runBlocking<Unit> {
        val runtime = ScriptRuntimeFactory(ScriptNetwork(OkHttpClient()) { "test" }).open(
            ScriptRuntimeSpec(
                name = "stall",
                scripts = listOf(ScriptSource("stall.js", "function hang() { return new Promise(function () {}); }")),
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

    // Sync host functions run on the engine's thread, where the profile of a runtime kept for another profile than the
    // active one (a library update) wasn't set: storage and settings reads went to the active profile
    @Test
    fun `sync and async host functions both run in the runtime's profile`() = runBlocking<Unit> {
        val runtime = ScriptRuntimeFactory(ScriptNetwork(OkHttpClient()) { "test" }).open(
            ScriptRuntimeSpec(
                name = "profile",
                scripts = listOf(
                    ScriptSource(
                        "profile.js",
                        "async function profiles() { return [syncProfile(), await asyncProfile()]; }",
                    ),
                ),
                bindings = {
                    function("syncProfile") { SourcePreferenceProfile.current }
                    asyncFunction("asyncProfile") { SourcePreferenceProfile.current }
                },
                hostContext = SourcePreferenceProfile.of(7),
            ),
        )

        runtime.use { it.call("profiles", noArgs) } shouldBe JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(7)))
    }
}
