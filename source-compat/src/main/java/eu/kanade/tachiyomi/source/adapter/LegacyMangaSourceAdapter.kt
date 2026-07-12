package eu.kanade.tachiyomi.source.adapter

import android.content.SharedPreferences
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.PreferenceScreen
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.asEntryImagePage
import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryItemOrientationProvider
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.entry.IncrementalChapterSource
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.WebViewSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.sourceItemOrientation
import eu.kanade.tachiyomi.source.sourcePreferences
import eu.kanade.tachiyomi.source.toEntryFilterList
import eu.kanade.tachiyomi.source.toEntryItemOrientation
import eu.kanade.tachiyomi.source.toEntryUpdateStrategy
import eu.kanade.tachiyomi.source.toLegacyFilterList
import eu.kanade.tachiyomi.source.toLegacyPage
import eu.kanade.tachiyomi.source.toLegacyUpdateStrategy
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import eu.kanade.tachiyomi.source.ConfigurableSource as LegacyConfigurableSource

/**
 * Wraps a legacy manga [Source] as a [UnifiedSource].
 *
 * All returned items report [EntryType.MANGA]. [CatalogueSource] and
 * [LegacyConfigurableSource] capabilities are bridged so that catalogue browsing
 * and extension preferences continue to work under their existing keys.
 */
open class LegacyMangaSourceAdapter(
    val source: Source,
) : UnifiedSource, EntryItemOrientationProvider, IncrementalChapterSource, ChapterNumberRecognitionSource {

    override val id: Long get() = source.id
    override val name: String get() = source.name
    override val itemOrientation: EntryItemOrientation
        get() = source.sourceItemOrientation().toEntryItemOrientation()

    override fun getFilterList(): EntryFilterList = source.getFilterList().toEntryFilterList()

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
        source.getPopularManga(page).toEntryPageResult()

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> =
        source.getLatestUpdates(page).toEntryPageResult()

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> =
        source.getSearchManga(page, query, filters.toLegacyFilterList()).toEntryPageResult()

    override suspend fun getContentDetails(entry: SEntry): SEntry {
        require(entry.type == EntryType.MANGA) {
            "Legacy manga source only supports manga entries"
        }
        val update = source.getMangaUpdate(
            manga = entry.toSManga(),
            chapters = emptyList(),
            fetchDetails = true,
            fetchChapters = false,
        )
        return update.manga.toSEntry(fallback = entry)
    }

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> {
        return getChapterList(entry, emptyList())
    }

    override suspend fun getChapterList(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
    ): List<SEntryChapter> {
        require(entry.type == EntryType.MANGA) {
            "Legacy manga source only supports manga entries"
        }
        val manga = entry.toSManga()
        val update = source.getMangaUpdate(
            manga = manga,
            chapters = existingChapters.map { it.toSChapter() },
            fetchDetails = false,
            fetchChapters = true,
        )
        return update.chapters.map { chapter ->
            if (source is HttpSource) {
                @Suppress("DEPRECATION")
                source.prepareNewChapter(chapter, manga)
            }
            chapter.toSEntryChapter()
        }
    }

    override suspend fun getMedia(
        chapter: SEntryChapter,
        selection: PlaybackSelection,
    ): EntryMedia {
        return EntryMedia.ImagePages(
            pages = source.getPageList(chapter.toSChapter()).map { it.asEntryImagePage() },
        )
    }
}

/**
 * Wraps a legacy manga [CatalogueSource] as an [EntryCatalogueSource].
 */
open class LegacyMangaCatalogueSourceAdapter(
    source: CatalogueSource,
) : LegacyMangaSourceAdapter(source), EntryCatalogueSource {

    private val catalogueSource: CatalogueSource get() = source as CatalogueSource

    override val lang: String get() = catalogueSource.lang
    override val supportsLatest: Boolean get() = catalogueSource.supportsLatest
    override val supportsImmersiveFeed: Boolean
        get() = try {
            catalogueSource.supportsImmersiveFeed
        } catch (_: AbstractMethodError) {
            false
        }
}

/**
 * Wraps a legacy manga [HttpSource] and exposes Entry-era browser capabilities.
 */
open class LegacyMangaWebViewCatalogueSourceAdapter(
    source: HttpSource,
) : LegacyMangaCatalogueSourceAdapter(source), EntryImageSource, ChapterWebViewSource, SourceHomePage {

    private val httpSource: HttpSource get() = source as HttpSource

    override val client: OkHttpClient get() = httpSource.client
    override val headers: Headers get() = httpSource.headers

    override fun getContentUrl(entry: SEntry): String =
        httpSource.getMangaUrl(entry.toSManga())

    override fun getChapterUrl(chapter: SEntryChapter): String =
        httpSource.getChapterUrl(chapter.toSChapter())

    override fun getWebViewHeaders(): Map<String, String> =
        httpSource.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

    override fun getHomeUrl(): String? =
        httpSource.getHomeUrl()

    override suspend fun getImageUrl(page: EntryImagePage): String =
        page.imageUrl ?: httpSource.getImageUrl(page.toLegacyPage())

    override fun imageRequest(page: EntryImagePage, imageUrl: String): Request =
        GET(imageUrl, headers)

    override suspend fun getImage(page: EntryImagePage, progress: ProgressListener?): Response =
        httpSource.getImage(page.toLegacyPage(progress))
}

/**
 * Wraps a legacy manga [LegacyConfigurableSource] as a unified [ConfigurableSource].
 */
open class LegacyMangaConfigurableSourceAdapter(
    source: Source,
) : LegacyMangaSourceAdapter(source), ConfigurableSource {

    private val configurableSource: LegacyConfigurableSource
        get() = source as LegacyConfigurableSource

    override fun getSourcePreferences(): SharedPreferences =
        configurableSource.sourcePreferences()

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        configurableSource.setupPreferenceScreen(screen)
    }
}

/**
 * Wraps a legacy manga source that is both a [CatalogueSource] and a
 * [LegacyConfigurableSource].
 */
open class LegacyMangaConfigurableCatalogueSourceAdapter(
    source: CatalogueSource,
) : LegacyMangaCatalogueSourceAdapter(source), ConfigurableSource {

    private val configurableSource: LegacyConfigurableSource
        get() = source as LegacyConfigurableSource

    override fun getSourcePreferences(): SharedPreferences =
        configurableSource.sourcePreferences()

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        configurableSource.setupPreferenceScreen(screen)
    }
}

/**
 * Wraps a legacy manga [HttpSource] that also has source preferences.
 */
class LegacyMangaWebViewConfigurableCatalogueSourceAdapter(
    source: HttpSource,
) : LegacyMangaConfigurableCatalogueSourceAdapter(source), EntryImageSource, ChapterWebViewSource, SourceHomePage {

    private val httpSource: HttpSource get() = source as HttpSource

    override val client: OkHttpClient get() = httpSource.client
    override val headers: Headers get() = httpSource.headers

    override fun getContentUrl(entry: SEntry): String =
        httpSource.getMangaUrl(entry.toSManga())

    override fun getChapterUrl(chapter: SEntryChapter): String =
        httpSource.getChapterUrl(chapter.toSChapter())

    override fun getWebViewHeaders(): Map<String, String> =
        httpSource.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

    override fun getHomeUrl(): String? =
        httpSource.getHomeUrl()

    override suspend fun getImageUrl(page: EntryImagePage): String =
        page.imageUrl ?: httpSource.getImageUrl(page.toLegacyPage())

    override fun imageRequest(page: EntryImagePage, imageUrl: String): Request =
        GET(imageUrl, headers)

    override suspend fun getImage(page: EntryImagePage, progress: ProgressListener?): Response =
        httpSource.getImage(page.toLegacyPage(progress))
}

/**
 * Adapts a legacy manga [Source] to the unified source contract.
 *
 * The returned instance implements [EntryCatalogueSource] and/or
 * [ConfigurableSource] when the legacy source supports those capabilities.
 */
fun Source.asUnifiedSource(): UnifiedSource = when {
    this is HttpSource && this is LegacyConfigurableSource ->
        LegacyMangaWebViewConfigurableCatalogueSourceAdapter(this)
    this is HttpSource -> LegacyMangaWebViewCatalogueSourceAdapter(this)
    this is CatalogueSource && this is LegacyConfigurableSource ->
        LegacyMangaConfigurableCatalogueSourceAdapter(this)
    this is CatalogueSource -> LegacyMangaCatalogueSourceAdapter(this)
    this is LegacyConfigurableSource -> LegacyMangaConfigurableSourceAdapter(this)
    else -> LegacyMangaSourceAdapter(this)
}

private fun MangasPage.toEntryPageResult(): EntryPageResult<SEntry> =
    EntryPageResult(mangas.map { it.toSEntry(fallback = null) }, hasNextPage)

private fun SManga.toSEntry(fallback: SEntry?): SEntry = SEntry.create().also {
    val fallbackUrl = fallback?.let { readInitialized { it.url } }
    val fallbackTitle = fallback?.let { readInitialized { it.title } }
    val resolvedUrl = readInitialized { url } ?: fallbackUrl.orEmpty()
    it.url = resolvedUrl
    it.title = readInitialized { title } ?: fallbackTitle ?: resolvedUrl
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = getGenres()
    it.status = status
    it.thumbnailUrl = thumbnail_url
    it.updateStrategy = update_strategy.toEntryUpdateStrategy()
    it.initialized = initialized
    it.memo = memo
    it.type = EntryType.MANGA
}

private inline fun <T> readInitialized(block: () -> T): T? {
    return try {
        block()
    } catch (_: UninitializedPropertyAccessException) {
        null
    }
}

private fun SEntry.toSManga(): SManga = SManga.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre?.joinToString(", ")
    it.status = status
    it.thumbnail_url = thumbnailUrl
    it.update_strategy = updateStrategy?.toLegacyUpdateStrategy() ?: UpdateStrategy.ALWAYS_UPDATE
    it.initialized = initialized
    it.memo = memo
}

private fun SChapter.toSEntryChapter(): SEntryChapter = SEntryChapter.create().also {
    it.url = url
    it.name = name
    it.dateUpload = date_upload
    it.chapterNumber = chapter_number.toDouble()
    it.scanlator = scanlator
    it.memo = memo
}

private fun SEntryChapter.toSChapter(): SChapter = SChapter.create().also {
    it.url = url
    it.name = name
    it.date_upload = dateUpload
    it.chapter_number = chapterNumber.toFloat()
    it.scanlator = scanlator
    it.memo = memo
}
