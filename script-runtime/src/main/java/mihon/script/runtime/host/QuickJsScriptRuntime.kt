package mihon.script.runtime.host

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.QuickJsException
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import mihon.script.runtime.ScriptBindings
import mihon.script.runtime.ScriptException
import mihon.script.runtime.ScriptRuntime
import mihon.script.runtime.ScriptRuntimeSpec
import mihon.script.runtime.ScriptSource
import mihon.script.runtime.engine.BytecodeCache
import mihon.script.runtime.engine.EvaluationThreads
import kotlin.time.Duration.Companion.seconds

/**
 * A [ScriptRuntime] on one QuickJS engine. Opening evaluates `host.js`, the spec's scripts and the caller's
 * bindings; a call evaluates `await __scriptCall(name, argsJson)`, which settles the script's promise into JSON.
 */
internal class QuickJsScriptRuntime private constructor(
    private val spec: ScriptRuntimeSpec,
    private val engine: QuickJs,
) : ScriptRuntime {
    private val mutex = Mutex()

    @Volatile
    private var currentCall: HostCall? = null

    override val isOpen: Boolean
        get() = !engine.isClosed

    override suspend fun call(function: String, args: JsonArray): JsonElement = mutex.withLock {
        check(isOpen) { "${spec.name} is closed" }
        val script = "await __scriptCall(${JsonPrimitive(function)}, ${JsonPrimitive(args.toString())})"
        val outcome = Json.parseToJsonElement(evaluate(script)).jsonObject
        if (outcome["ok"]?.jsonPrimitive?.contentOrNull == "true") {
            outcome["value"] ?: JsonNull
        } else {
            throw outcome.toScriptError()
        }
    }

    override fun close() {
        currentCall?.end()
        engine.close()
    }

    /** Evaluates [script] as one call: its own host work, the stall limit, the ceiling, and closing when stopped. */
    private suspend fun evaluate(script: String): String {
        val call = HostCall(spec.hostContext)
        currentCall = call
        var stalled = false
        try {
            return withContext(EvaluationThreads.dispatcher) {
                withTimeout(spec.limits.ceiling) {
                    coroutineScope {
                        val result = async { engine.evaluate<String>(script) }
                        val watchdog = launch {
                            while (true) {
                                delay(WATCHDOG_PERIOD.coerceAtMost(spec.limits.stallTimeout))
                                if (call.idleFor >= spec.limits.stallTimeout) {
                                    stalled = true
                                    result.cancel()
                                    return@launch
                                }
                            }
                        }
                        try {
                            result.await()
                        } finally {
                            watchdog.cancel()
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            close()
            throw ScriptException.TimedOut(spec.name)
        } catch (e: CancellationException) {
            close()
            if (stalled) throw ScriptException.Stalled(spec.name)
            throw e
        } catch (e: QuickJsException) {
            // An error the call wrapper couldn't catch, such as running out of memory while reporting another
            if (e.isOutOfMemory()) throw ScriptException.OutOfMemory(spec.name)
            throw ScriptException.Thrown(e::class.simpleName ?: "Error", e.message.orEmpty(), e.stack)
        } finally {
            call.end()
            currentCall = null
        }
    }

    private fun JsonObject.toScriptError(): ScriptException {
        val name = get("name")?.jsonPrimitive?.contentOrNull ?: "Error"
        val message = get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
        if (name == "InternalError" && message == OUT_OF_MEMORY) return ScriptException.OutOfMemory(spec.name)
        return ScriptException.Thrown(name, message, get("stack")?.jsonPrimitive?.contentOrNull)
    }

    private fun QuickJsException.isOutOfMemory() = message?.contains(OUT_OF_MEMORY) == true

    private inner class Bindings : ScriptBindings {
        val asyncNames = mutableListOf<String>()

        override fun function(name: String, block: (args: List<Any?>) -> Any?) {
            engine.function(name) { block(it.asList()) }
        }

        override fun asyncFunction(name: String, block: suspend (args: List<Any?>) -> Any?) {
            asyncNames += name
            engine.asyncFunction(name) { args ->
                val call = currentCall ?: throw IllegalStateException("${spec.name}: $name called outside a call")
                call.run { block(args.asList()) }
            }
        }
    }

    private suspend fun load() {
        engine.function("__hostCallEnded") { currentCall?.end() }
        engine.function("__hostScriptSource") { args ->
            val name = args[0] as String
            spec.lazyScripts[name]?.code ?: throw IllegalArgumentException("${spec.name} has no script $name")
        }
        val bindings = Bindings().apply(spec.bindings)
        mutex.withLock {
            evaluateScript(hostScript)
            evaluate("__scriptWrapAsync(${JsonArray(bindings.asyncNames.map(::JsonPrimitive))}); ''")
            spec.scripts.forEach { evaluateScript(it) }
        }
    }

    private suspend fun evaluateScript(source: ScriptSource) {
        val bytecode = source.compiled(engine)
        val call = HostCall(spec.hostContext)
        currentCall = call
        try {
            withContext(EvaluationThreads.dispatcher) {
                withTimeout(spec.limits.ceiling) { engine.evaluate<Any?>(bytecode) }
            }
        } finally {
            call.end()
            currentCall = null
        }
    }

    companion object {
        const val LOG_TAG = "ScriptRuntime"
        private const val OUT_OF_MEMORY = "out of memory"
        private val WATCHDOG_PERIOD = 1.seconds

        private val hostScript by lazy { ScriptResources.source("host.js") }

        suspend fun open(spec: ScriptRuntimeSpec): QuickJsScriptRuntime = withContext(EvaluationThreads.dispatcher) {
            val engine = QuickJs.create(EvaluationThreads.dispatcher).apply {
                memoryLimit = spec.limits.memoryBytes
                maxStackSize = spec.limits.stackBytes.coerceAtMost(EvaluationThreads.MAX_STACK_SIZE)
                unhandledRejectionHandler = { error ->
                    logcat(LOG_TAG, LogPriority.WARN) { "${spec.name}: unhandled promise rejection\n${error.asLog()}" }
                }
            }
            val runtime = QuickJsScriptRuntime(spec, engine)
            try {
                runtime.load()
                runtime
            } catch (e: Exception) {
                runtime.close()
                if (e is CancellationException) throw e
                throw ScriptException.LoadFailed(spec.name, e)
            }
        }

        private fun ScriptSource.compiled(engine: QuickJs): ByteArray =
            BytecodeCache.get(this) { code, name -> engine.compile(code, name) }
    }
}
