package eu.kanade.tachiyomi.source.lnreader

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import mihon.script.runtime.ScriptNetwork
import mihon.script.runtime.ScriptRuntimeFactory
import mihon.script.runtime.ScriptRuntimePool

/**
 * Runs LNReader plugins, each in runtimes of its own per profile. A runtime keeps the plugin object between calls, so
 * state the plugin keeps in memory (tokens, caches) lasts until the runtime closes: after a couple of idle minutes,
 * when a call is stopped, or on [invalidate].
 */
@Inject
@SingleIn(AppScope::class)
class LnReaderPlugins(
    factory: ScriptRuntimeFactory,
    private val network: ScriptNetwork,
    private val preferences: EntryPreferenceProvider,
) {
    private val pool = ScriptRuntimePool(factory)

    /**
     * Runs [block] on [plugin] loaded for [profileId]: its host work, storage included, reads that profile's settings.
     * A changed plugin file gets new runtimes; the old ones close when idle.
     */
    suspend fun <T> use(plugin: LnReaderPluginFile, profileId: Long, block: suspend (LnReaderPlugin) -> T): T {
        val key = RuntimeKey(plugin.sourceId, profileId, plugin.source.sha256)
        return pool.use(key, {
            lnReaderRuntimeSpec(plugin, profileId, network, preferences)
        }) { block(LnReaderPlugin(it)) }
    }

    /** Reloads the plugin of [sourceId] on its next call, in every profile; its constructor reads storage again. */
    fun invalidate(sourceId: Long) = pool.invalidate { it is RuntimeKey && it.sourceId == sourceId }

    private data class RuntimeKey(val sourceId: Long, val profileId: Long, val sha256: String)
}
