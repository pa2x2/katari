package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimeRelation
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

interface AnimeCatalogueSource : AnimeSource {

    override val lang: String

    override val supportsLatest: Boolean

    // extensions-lib 14 declared these three abstract with Rx fallbacks; no extension in use relies on them
    override suspend fun getPopularAnime(page: Int): AnimesPage = throw UnsupportedOperationException()

    override suspend fun getLatestUpdates(page: Int): AnimesPage = throw UnsupportedOperationException()

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage =
        throw UnsupportedOperationException()

    override suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate = supervisorScope {
        val details = if (fetchDetails) async { getAnimeDetails(anime) } else null
        val newEpisodes = if (fetchEpisodes) async { getEpisodeList(anime) } else null
        SAnimeEpisodeUpdate(details?.await() ?: anime, newEpisodes?.await() ?: episodes)
    }

    override suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate = supervisorScope {
        val details = if (fetchDetails) async { getAnimeDetails(anime) } else null
        val newSeasons = if (fetchSeasons) async { getSeasonList(anime) } else null
        SAnimeSeasonUpdate(details?.await() ?: anime, newSeasons?.await() ?: seasons)
    }

    override suspend fun getRelatedAnimeList(anime: SAnime): List<AnimeRelation> =
        throw UnsupportedOperationException()

    // extensions-lib 17 overrides these two here, which gives this interface the static
    // getHosterList$suspendImpl and getVideoList$suspendImpl that its compiled subclasses may call
    override suspend fun getHosterList(episode: SEpisode): List<Hoster> = throw UnsupportedOperationException()

    override suspend fun getVideoList(hoster: Hoster): List<Video> = throw UnsupportedOperationException()

    /** Komikku: whether [fetchRelatedAnimeList] returns entries the site itself relates to this one. */
    val supportsRelatedAnimes: Boolean
        get() = false

    /** Komikku: skip the related entries found by searching for words of the title. */
    val disableRelatedAnimesBySearch: Boolean
        get() = false

    val disableRelatedAnimes: Boolean
        get() = false

    override suspend fun getRelatedAnimeList(
        anime: SAnime,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) {
        if (disableRelatedAnimes) return
        val handler = CoroutineExceptionHandler { _, e -> exceptionHandler(e) }
        supervisorScope {
            if (supportsRelatedAnimes) launch(handler) { getRelatedAnimeListByExtension(anime, pushResults) }
            if (!disableRelatedAnimesBySearch) launch(handler) { getRelatedAnimeListBySearch(anime, pushResults) }
        }
    }

    suspend fun getRelatedAnimeListByExtension(
        anime: SAnime,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) {
        runCatching { fetchRelatedAnimeList(anime) }
            .onSuccess { if (it.isNotEmpty()) pushResults(Pair("", it), false) }
            .onFailure { e -> logcat(LogPriority.ERROR, e) { "Related anime from the extension failed" } }
    }

    suspend fun fetchRelatedAnimeList(anime: SAnime): List<SAnime> = throw UnsupportedOperationException()

    /** The words of a title worth searching for: no punctuation, numbers alone or single characters. */
    fun String.stripKeywordForRelatedAnimes(): List<String> {
        val regexWhitespace = Regex("\\s+")
        val regexSpecialCharacters =
            Regex("([!~#$%^&*+_|/\\\\,?:;'“”‘’\"<>(){}\\[\\]。・～：—！？、―«»《》〘〙【】「」｜]|\\s-|-\\s|\\s\\.|\\.\\s)")
        val regexNumberOnly = Regex("^\\d+$")
        return replace(regexSpecialCharacters, " ")
            .split(regexWhitespace)
            .map { it.replace(regexNumberOnly, "").lowercase() }
            .filter { it.length > 1 }
    }

    suspend fun getRelatedAnimeListBySearch(
        anime: SAnime,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) {
        val words = linkedSetOf(anime.title)
        anime.title.stripKeywordForRelatedAnimes()
            .filterNot { word -> words.any { it.lowercase() == word } }
            .forEach { words.add(it) }
        coroutineScope {
            val filters = getFilterList()
            words.forEach { keyword ->
                launch {
                    runCatching { getSearchAnime(1, keyword, filters).animes }
                        .onSuccess { if (it.isNotEmpty()) pushResults(Pair(keyword, it), false) }
                        .onFailure { e -> logcat(LogPriority.ERROR, e) { "Related anime search failed" } }
                }
            }
        }
    }
}
