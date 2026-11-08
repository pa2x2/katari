package app.katari.script.runtime.host

import app.katari.script.runtime.ScriptSource

/** Scripts shipped as Java resources of this module, readable both in the app and in host tests. */
internal object ScriptResources {
    fun source(name: String): ScriptSource {
        val path = "app/katari/script/runtime/$name"
        val stream = ScriptResources::class.java.classLoader!!.getResourceAsStream(path)
            ?: throw IllegalStateException("Missing script resource $path")
        return ScriptSource(name, stream.use { it.readBytes().decodeToString() })
    }
}
