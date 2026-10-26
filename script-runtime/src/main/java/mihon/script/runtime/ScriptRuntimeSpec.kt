package mihon.script.runtime

import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * What a runtime is opened with: the scripts it evaluates in order, scripts it evaluates only when one of them asks
 * (`__scriptLoad(name)`), the caller's host functions, and its limits.
 *
 * [hostContext] is the context every host function and request of the runtime runs in. A caller that keeps one
 * runtime per profile passes that profile here, so host work never reads whichever profile happens to be active.
 */
class ScriptRuntimeSpec(
    val name: String,
    val scripts: List<ScriptSource>,
    val lazyScripts: Map<String, ScriptSource> = emptyMap(),
    val bindings: ScriptBindings.() -> Unit = {},
    val limits: ScriptLimits = ScriptLimits(),
    val hostContext: CoroutineContext = EmptyCoroutineContext,
)

/** A script and the name its errors and stack traces show. Its bytecode is cached per process by [sha256]. */
class ScriptSource(val name: String, val code: String) {
    val sha256: String by lazy {
        MessageDigest.getInstance("SHA-256").digest(code.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

/**
 * A call is stopped when no host work started or finished for [stallTimeout], or when it runs past [ceiling].
 * Busy JavaScript makes no host progress, so [stallTimeout] must outlast the longest parse a script does.
 */
data class ScriptLimits(
    val memoryBytes: Long = 128L shl 20,
    val stackBytes: Long = 1L shl 20,
    val stallTimeout: Duration = 60.seconds,
    val ceiling: Duration = 5.minutes,
)

/**
 * The caller's host functions. Arguments and results cross as QuickJS maps them: `null`, [Boolean], [Long],
 * [Double], [String], [List], [Map], `UByteArray` for a `Uint8Array`.
 *
 * An async function runs in the runtime's host context, counts as progress for the stall limit, and is cancelled when
 * the call it belongs to has its result; the script then never sees it settle.
 */
interface ScriptBindings {
    fun function(name: String, block: (args: List<Any?>) -> Any?)

    fun asyncFunction(name: String, block: suspend (args: List<Any?>) -> Any?)
}
