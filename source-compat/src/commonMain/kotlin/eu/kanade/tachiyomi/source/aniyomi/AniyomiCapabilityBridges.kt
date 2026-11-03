package eu.kanade.tachiyomi.source.aniyomi

import android.content.SharedPreferences
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.aniyomi.capability.relatedTitlesCall
import eu.kanade.tachiyomi.source.aniyomi.entry.toSAnime
import eu.kanade.tachiyomi.source.aniyomi.entry.toSEpisode
import eu.kanade.tachiyomi.source.aniyomi.related.relatedTitles
import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryPreferenceScreen
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.RelatedEntryGroup
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.entry.getEntryPreferenceProvider
import eu.kanade.tachiyomi.source.failure.runExtensionCode
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request

/*
 * Capabilities only some Aniyomi sources have. Each bridge reaches the extension through AniyomiSourceHolder and is
 * mixed only into adapters whose source has the matching Aniyomi type, so the casts hold.
 */

/** An extension's settings, read and written in the file the extension itself opens: `source_<its own ID>`. */
internal interface AniyomiSettingsBridge : ConfigurableSource, AniyomiSourceHolder {

    override fun getSourcePreferences(): SharedPreferences = getEntryPreferenceProvider().preferences(source.id)

    override fun setupPreferenceScreen(screen: EntryPreferenceScreen) {
        runExtensionCode { (source as ConfigurableAnimeSource).setupPreferenceScreen(screen) }
    }
}

/**
 * A website source's pages for WebView and the browser, and its client and headers, which cover and episode preview
 * images load with, as in Aniyomi.
 */
internal interface AniyomiWebBridge : ChapterWebViewSource, SourceHomePage, EntryImageSource, AniyomiSourceHolder {

    private val httpSource: AnimeHttpSource get() = source as AnimeHttpSource

    override val client: OkHttpClient get() = runExtensionCode { httpSource.client }
    override val headers: Headers get() = runExtensionCode { httpSource.headers }

    override fun getContentUrl(entry: SEntry): String = runExtensionCode { httpSource.getAnimeUrl(entry.toSAnime()) }

    override fun getChapterUrl(chapter: SEntryChapter): String =
        runExtensionCode { httpSource.getEpisodeUrl(chapter.toSEpisode()) }

    override fun getWebViewHeaders(): Map<String, String> =
        headers.toMultimap().mapValues { it.value.getOrNull(0).orEmpty() }

    override fun getHomeUrl(): String? = runExtensionCode { httpSource.getHomeUrl() }

    override fun imageRequest(page: EntryImagePage, imageUrl: String): Request = GET(imageUrl, headers)
}

/** Titles the extension itself relates to another; offered only where AniyomiOverrides finds a way to ask. */
internal interface AniyomiRelatedBridge : RelatedEntriesSource, AniyomiSourceHolder {

    override suspend fun getRelatedEntries(entry: SEntry): List<RelatedEntryGroup> = callExtension {
        source.relatedTitles(entry, checkNotNull(source.relatedTitlesCall))
    }
}
