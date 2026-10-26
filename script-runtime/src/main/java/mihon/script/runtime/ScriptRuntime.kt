package mihon.script.runtime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * A long-lived script runtime. One call runs at a time; [call] waits for the one before it.
 *
 * A call that the runtime stops ([ScriptException.Stalled], [ScriptException.TimedOut]) or that the caller cancels
 * closes the runtime: the engine keeps the jobs the stopped script queued, so the next call needs a fresh one.
 */
interface ScriptRuntime : AutoCloseable {
    val isOpen: Boolean

    /**
     * Calls the global function [function] with [args] and returns its awaited result as JSON (`undefined` → `null`).
     * Arguments and the result cross as JSON text, never as script source.
     */
    suspend fun call(function: String, args: JsonArray): JsonElement
}

sealed class ScriptException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The script threw or rejected: [jsName] is the error's `name`, the message its `message`. */
    class Thrown(val jsName: String, message: String, val jsStack: String?) : ScriptException(message)

    /**
     * Busy with no host work started or finished for the stall limit, or waiting on a promise nothing is left to
     * settle (stopped at once).
     */
    class Stalled(runtime: String) : ScriptException("$runtime made no progress")

    class TimedOut(runtime: String) : ScriptException("$runtime ran past its time limit")

    class OutOfMemory(runtime: String) : ScriptException("$runtime ran out of memory")

    class LoadFailed(runtime: String, cause: Throwable) : ScriptException("$runtime couldn't be loaded", cause)
}
