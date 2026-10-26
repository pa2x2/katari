package mihon.script.runtime.web

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import logcat.logcat
import mihon.script.runtime.ScriptBindings
import mihon.script.runtime.host.QuickJsScriptRuntime
import java.util.concurrent.ConcurrentHashMap

/**
 * The host side of `TextDecoder`, timers and `console` (`web/text.js`, `web/timers.js`, `web/console.js`). A timer is
 * an async host function, so it counts as progress and is cancelled with its call; `clearTimeout` completes it early
 * instead of letting it sleep out its delay.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal class LanguageShims(private val runtimeName: String) {
    private val timers = ConcurrentHashMap<Int, CompletableDeferred<Unit>>()

    fun install(bindings: ScriptBindings) = with(bindings) {
        function("__hostEncodingName") { args -> TextEncodings.forLabel(args[0] as String)?.first }
        function("__hostDecode") { args ->
            val (_, charset) = TextEncodings.forLabel(args[1] as String)
                ?: throw IllegalArgumentException("Unsupported encoding ${args[1]}")
            TextEncodings.decode((args[0] as UByteArray).asByteArray(), charset, args[2] == true, args[3] == true)
        }
        asyncFunction("__hostTimer") { args ->
            val id = (args[0] as Number).toInt()
            val cleared = CompletableDeferred<Unit>()
            timers[id] = cleared
            try {
                withTimeoutOrNull((args[1] as Number).toLong().coerceAtLeast(0)) { cleared.await() } == null
            } finally {
                timers.remove(id)
            }
        }
        function("__hostTimerClear") { args ->
            timers.remove((args[0] as Number).toInt())?.complete(Unit)
            null
        }
        function("__hostLog") { args ->
            logcat(QuickJsScriptRuntime.LOG_TAG, LogPriority.DEBUG) { "$runtimeName: ${args[0]}: ${args[1]}" }
            null
        }
    }
}
