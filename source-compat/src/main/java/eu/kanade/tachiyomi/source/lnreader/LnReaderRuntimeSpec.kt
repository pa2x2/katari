package eu.kanade.tachiyomi.source.lnreader

import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import eu.kanade.tachiyomi.source.lnreader.storage.LnReaderStorage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mihon.script.runtime.ScriptLimits
import mihon.script.runtime.ScriptNetwork
import mihon.script.runtime.ScriptRuntimeSpec
import mihon.script.runtime.ScriptSource
import tachiyomi.core.common.preference.SourcePreferenceProfile
import java.nio.charset.Charset
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalUnsignedTypes::class)
internal fun lnReaderRuntimeSpec(
    plugin: LnReaderPluginFile,
    profileId: Long,
    network: ScriptNetwork,
    preferences: EntryPreferenceProvider,
) =
    ScriptRuntimeSpec(
        name = plugin.source.name,
        scripts = LnReaderScripts.host + plugin.source,
        lazyScripts = LnReaderScripts.lazyLibs,
        bindings = {
            LnReaderStorage { preferences.preferences(plugin.sourceId) }.install(this)
            function("__lnreaderUserAgent") { network.userAgent() }
            function("__lnreaderDecodeText") { (bytes, charset) ->
                String((bytes as UByteArray).asByteArray(), Charset.forName(charset as String))
            }
            function("__lnreaderDeviceLocale") {
                buildJsonObject {
                    put("timeZone", TimeZone.getDefault().id)
                    put("locale", Locale.getDefault().toLanguageTag())
                }.toString()
            }
        },
        // A chapter with its pictures inline in the HTML runs to 17 M characters, and matching a regular expression over
        // it outgrows the default
        limits = ScriptLimits(memoryBytes = 192L shl 20),
        hostContext = SourcePreferenceProfile.of(profileId),
    )

/** The LNReader layer's scripts, read once per process from this module's resources. */
private object LnReaderScripts {
    val host by lazy {
        listOf("libs/core.js", "modules.js", "intl.js", "fetch.js", "storage.js", "plugin.js").map(::resource)
    }

    // Keyed by the name modules.js passes to __scriptLoad
    val lazyLibs by lazy { listOf("urlencode", "protobuf", "ciphers").associateWith { resource("libs/$it.js") } }

    private fun resource(name: String): ScriptSource {
        val path = "eu/kanade/tachiyomi/source/lnreader/$name"
        val stream = LnReaderScripts::class.java.classLoader!!.getResourceAsStream(path)
            ?: throw IllegalStateException("Missing script resource $path")
        return ScriptSource("lnreader/$name", stream.use { it.readBytes().decodeToString() })
    }
}
