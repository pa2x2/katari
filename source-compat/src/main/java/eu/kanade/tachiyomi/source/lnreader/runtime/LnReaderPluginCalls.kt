package eu.kanade.tachiyomi.source.lnreader.runtime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import tachiyomi.core.common.preference.SourcePreferenceProfile

/**
 * One plugin as its source calls it. A call runs in the profile of the work that started it, such as a library update
 * or a download, else in the active profile, so the plugin reads that profile's settings and storage.
 */
internal class LnReaderPluginCalls(
    private val file: LnReaderPluginFile,
    private val plugins: LnReaderPlugins,
    private val activeProfileId: () -> Long,
) {
    val profileId: Long
        get() = SourcePreferenceProfile.current ?: activeProfileId()

    suspend fun call(method: String, vararg args: JsonElement): JsonElement =
        plugins.use(file, profileId) { it.call(method, JsonArray(args.asList())) }

    suspend fun resolveUrls(paths: List<String>, isNovel: Boolean): List<String?> {
        if (paths.isEmpty()) return emptyList()
        return plugins.use(file, profileId) { it.resolveUrls(paths, isNovel) }
    }

    /** Drops the plugin's runtimes in every profile, so its next call constructs it again. */
    fun reload() = plugins.invalidate(file.sourceId)
}
