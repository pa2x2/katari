package app.katari.script.runtime

import app.katari.script.runtime.host.QuickJsScriptRuntime
import dev.zacsweers.metro.Inject

/** Opens script runtimes. Callers that keep runtimes between calls do so through a [ScriptRuntimePool]. */
@Inject
class ScriptRuntimeFactory(private val network: ScriptNetwork) {
    /** Throws [ScriptException.LoadFailed] when a script of [spec] fails to evaluate. */
    suspend fun open(spec: ScriptRuntimeSpec): ScriptRuntime = QuickJsScriptRuntime.open(spec, network)
}
