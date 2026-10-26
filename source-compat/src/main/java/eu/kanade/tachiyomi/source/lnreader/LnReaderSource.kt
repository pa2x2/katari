package eu.kanade.tachiyomi.source.lnreader

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.ArtworkSource
import eu.kanade.tachiyomi.source.entry.ChapterListPart
import eu.kanade.tachiyomi.source.entry.ChapterListPartsSource
import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryPreferenceScreen
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SiteStorage
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import eu.kanade.tachiyomi.source.entry.getEntryPreferenceProvider
import eu.kanade.tachiyomi.source.filter.withSourceFilterValues
import eu.kanade.tachiyomi.source.lnreader.catalogue.LnReaderCatalogue
import eu.kanade.tachiyomi.source.lnreader.catalogue.lnReaderFilterList
import eu.kanade.tachiyomi.source.lnreader.catalogue.lnReaderFilterValues
import eu.kanade.tachiyomi.source.lnreader.chapter.lnReaderChapterMedia
import eu.kanade.tachiyomi.source.lnreader.entry.LnReaderRefresh
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginCalls
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginFile
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import eu.kanade.tachiyomi.source.lnreader.settings.LnReaderSetting
import eu.kanade.tachiyomi.source.lnreader.settings.LnReaderSettingStore
import eu.kanade.tachiyomi.source.lnreader.settings.addLnReaderSettings
import eu.kanade.tachiyomi.source.lnreader.web.LnReaderUrls
import eu.kanade.tachiyomi.source.lnreader.web.saveSiteStorage
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Headers
import okhttp3.OkHttpClient

/**
 * A book source over one LNReader plugin, under [id], the key Katari stores it under; the plugin's own ID names its
 * settings file. This class holds what every plugin has. Katari tells capabilities apart by type, so settings and the
 * site's web storage, which only some plugins have, come from the subclasses LnReaderSourceFactory makes.
 *
 * Closing it drops the plugin's runtimes in every profile, for when its extension is updated or removed.
 */
open class LnReaderSource internal constructor(setup: LnReaderSourceSetup) :
    EntryCatalogueSource,
    SourceMetadata,
    ChapterListPartsSource,
    ArtworkSource,
    ChapterWebViewSource,
    SourceHomePage,
    ChapterNumberRecognitionSource,
    AutoCloseable {

    private val file = setup.file
    private val description = setup.description
    private val customCss = setup.customCss
    private val calls = setup.calls
    private val settings = setup.settings

    final override val id: Long = setup.key
    final override val lang: String = setup.lang
    final override val client: OkHttpClient = setup.client

    val pluginId: String get() = description.id
    val version: String get() = description.version
    val site: String get() = description.site

    final override val name: String = description.name.ifBlank { file.name }
    final override val supportsLatest: Boolean get() = description.readsLatest

    // Plugins search by text alone; their filters apply to the popular list
    final override val textSearchUsesFilters: Boolean get() = false
    final override val supportedEntryTypes: Set<EntryType> = setOf(EntryType.BOOK)

    /** The plugin's image headers, its User-Agent among them, which its site's covers may need. */
    final override val headers: Headers = Headers.Builder().apply {
        description.imageHeaders.forEach { (name, value) -> runCatching { add(name, value) } }
    }.build()

    private val urls = LnReaderUrls(site, calls, description.resolvesUrls)
    private val catalogue = LnReaderCatalogue(calls, urls)
    private val chapters = LnReaderRefresh(calls, urls, description.pagedChapters)

    final override fun getFilterList(): EntryFilterList = lnReaderFilterList(description.filters)

    final override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
        catalogue.popular(page, latest = false, filters = description.filters)

    final override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> =
        catalogue.popular(page, latest = true, filters = description.filters)

    final override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> {
        if (query.isNotBlank()) return catalogue.search(page, query)
        val definitions = description.filters
            ?: return catalogue.popular(page, latest = false, filters = null)
        return filters.withSourceFilterValues { bound ->
            catalogue.popular(page, latest = false, filters = lnReaderFilterValues(definitions, bound))
        }
    }

    final override suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): EntryRefresh = chapters.refresh(entry, existingChapters, fetchDetails, fetchChapters)

    final override suspend fun getOlderChapters(entry: SEntry, token: String): ChapterListPart =
        chapters.olderChapters(entry, token)

    final override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia =
        lnReaderChapterMedia(
            path = chapter.url,
            html = calls.call("parseChapter", JsonPrimitive(chapter.url)).asText().orEmpty(),
            site = site,
            imageHeaders = description.imageHeaders,
            customCss = customCss,
        )

    final override fun getContentUrl(entry: SEntry): String = urls.pageUrl(entry.url, entry.memo)

    final override fun getChapterUrl(chapter: SEntryChapter): String = urls.pageUrl(chapter.url, chapter.memo)

    final override fun getHomeUrl(): String? = site.takeIf(String::isNotBlank)

    /** Drops the plugin's runtimes in every profile, so its next call constructs it again, reading its settings anew. */
    fun reload() = calls.reload()

    final override fun close() = reload()

    /** The plugin's settings file in the active profile, which its settings and storage live in. */
    internal fun settingsFile(): SharedPreferences = getEntryPreferenceProvider().preferences(file.sourceId)

    internal fun setupSettings(screen: EntryPreferenceScreen) {
        screen.addLnReaderSettings(settings, LnReaderSettingStore(::settingsFile, settings, ::reload))
    }

    internal fun keepSiteStorage(storage: SiteStorage) {
        settingsFile().saveSiteStorage(storage)
        reload()
    }
}

/** What a source is made of: the plugin's [file] and [description], and the local [key] Katari stores it under. */
internal class LnReaderSourceSetup(
    val file: LnReaderPluginFile,
    val key: Long,
    val description: LnReaderPluginDescription,
    val lang: String,
    val customCss: String?,
    val calls: LnReaderPluginCalls,
    val client: OkHttpClient,
    val settings: List<LnReaderSetting>,
)
