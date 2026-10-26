package eu.kanade.tachiyomi.source.lnreader.runtime

import mihon.script.runtime.ScriptSource

/** A plugin's code as LNReader's repositories publish it; [name] is what its errors and stack traces show. */
class LnReaderPluginFile(val sourceId: Long, val name: String, code: String) {
    // As LNReader's loader wraps it; the newline keeps a trailing line comment from swallowing the return
    internal val source = ScriptSource(
        name,
        "__lnreaderRegister(function (require, module) { const exports = module.exports = {};\n$code\n;return exports.default;\n});",
    )
}
