package app.katari.showcase.extension

import app.katari.showcase.extension.catalog.ShowcaseCatalogClient
import app.katari.showcase.extension.catalog.pinnedSources
import eu.kanade.tachiyomi.source.entry.EntrySourceFactory
import eu.kanade.tachiyomi.source.entry.UnifiedSource

/**
 * Sources for the openly licensed works in showcase/content, served by the showcase content server.
 *
 * Only for the screenshot pipeline: the extension is never published, and it reads everything from a server that runs
 * beside the emulator.
 */
class ShowcaseFactory : EntrySourceFactory {
    override fun createSources(): List<UnifiedSource> {
        val catalog = ShowcaseCatalogClient(BuildConfig.CONTENT_SERVER)
        return pinnedSources().map { ShowcaseSource(it, catalog) }
    }
}
