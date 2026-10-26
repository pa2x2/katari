package mihon.script.runtime

import dev.zacsweers.metro.Inject
import mihon.script.runtime.host.QuickJsScriptRuntime

/** Opens script runtimes. Callers that keep runtimes between calls do so through a [ScriptRuntimePool]. */
@Inject
class ScriptRuntimeFactory(private val network: ScriptNetwork) {
    /** Throws [ScriptException.LoadFailed] when a script of [spec] fails to evaluate. */
    suspend fun open(spec: ScriptRuntimeSpec): ScriptRuntime = QuickJsScriptRuntime.open(spec, network)
}
