package eu.kanade.tachiyomi.source.lnreader

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.ArtworkSource
import eu.kanade.tachiyomi.source.entry.ChapterListPartsSource
import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import kotlinx.serialization.Serializable
import mihon.script.runtime.ScriptNetwork

/*
 * Stands in for the LNReader adapter's interface, as research 12 §4.2 of the LNReader compatibility workspace fixes
 * it, so the loader and store can be built and merged first. The adapter replaces this whole file. Until then every
 * call fails saying so, which installed plugins show as failing to load.
 */

private const val ADAPTER_UNAVAILABLE = "The LNReader adapter isn't available yet"

@Inject
class LnReaderSourceFactory(plugins: LnReaderPlugins, network: ScriptNetwork) {
    /** Loads [file] in the default profile and returns what the loader caches with it. */
    suspend fun describe(file: LnReaderPluginFile): LnReaderPluginDescription = TODO(ADAPTER_UNAVAILABLE)

    /** No runtime opens here; the first call loads the plugin in the calling profile. */
    fun create(
        file: LnReaderPluginFile,
        key: Long,
        description: LnReaderPluginDescription,
        lang: String,
        customCss: String?,
        activeProfileId: () -> Long,
    ): LnReaderSource = TODO(ADAPTER_UNAVAILABLE)
}

/** What a plugin says about itself. The loader caches it with the plugin's file, so it's serializable. */
@Serializable
class LnReaderPluginDescription(
    val id: String,
    val version: String,
)

/** A book source over one plugin. Closing it drops the plugin's runtimes in every profile. */
abstract class LnReaderSource :
    EntryCatalogueSource,
    SourceMetadata,
    ChapterListPartsSource,
    ArtworkSource,
    ChapterWebViewSource,
    SourceHomePage,
    ChapterNumberRecognitionSource,
    AutoCloseable {
    abstract val pluginId: String
    abstract val version: String
    abstract val site: String
}
