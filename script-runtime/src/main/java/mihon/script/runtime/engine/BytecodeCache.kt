package mihon.script.runtime.engine

import mihon.script.runtime.ScriptSource

/**
 * Compiled scripts, kept in memory for the life of the process and keyed by the source's SHA-256. Bytecode is tied to
 * the engine build that made it, so it is never written to disk, where an app update could meet a stale copy.
 * Bytecode compiled by one runtime runs in another.
 */
internal object BytecodeCache {
    // Bytecode is about three times its source; the library bundles alone are a few MB of it
    private const val MAX_BYTES = 48L shl 20

    private val entries = object : LinkedHashMap<String, ByteArray>(16, 0.75f, true) {}
    private var bytes = 0L

    fun get(source: ScriptSource, compile: (code: String, name: String) -> ByteArray): ByteArray {
        synchronized(this) { entries[source.sha256]?.let { return it } }
        val compiled = compile(source.code, source.name)
        synchronized(this) {
            entries.put(source.sha256, compiled)?.let { bytes -= it.size }
            bytes += compiled.size
            val oldest = entries.iterator()
            while (bytes > MAX_BYTES && oldest.hasNext()) {
                val evicted = oldest.next()
                if (evicted.key == source.sha256) continue
                bytes -= evicted.value.size
                oldest.remove()
            }
        }
        return compiled
    }
}
