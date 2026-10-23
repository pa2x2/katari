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

/**
 * The union of what extensions-lib 14, 16 and 17 and Komikku's fork declare here. Members an older lib declared
 * abstract and a newer one dropped or replaced get a default, so an extension built against any of them links.
 */
interface AnimeSource {

    /** The extension's own ID, not the key Katari stores the source under. */
    val id: Long

    val name: String

    val lang: String
        get() = ""

    val supportsLatest: Boolean

    fun getFilterList(): AnimeFilterList = AnimeFilterList()

    suspend fun getPopularAnime(page: Int): AnimesPage

    suspend fun getLatestUpdates(page: Int): AnimesPage

    suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage

    /** extensions-lib 17's single call for details and episodes; older extensions get it from the separate calls. */
    suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate

    suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate

    /** Whether [getRelatedAnimeList] may be called (extensions-lib 17). */
    val supportsRelatedAnime: Boolean
        get() = false

    suspend fun getRelatedAnimeList(anime: SAnime): List<AnimeRelation>

    /**
     * Komikku's related-anime call: results arrive through [pushResults] as (label, entries) groups, and failures
     * of single groups through [exceptionHandler].
     */
    suspend fun getRelatedAnimeList(
        anime: SAnime,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) {
    }

    suspend fun getHosterList(episode: SEpisode): List<Hoster> = throw UnsupportedOperationException()

    suspend fun getVideoList(hoster: Hoster): List<Video> = throw UnsupportedOperationException()

    suspend fun getSeasonList(anime: SAnime): List<SAnime> = throw UnsupportedOperationException()

    /** The extensions-lib 14 call, for sources without hosters. */
    suspend fun getVideoList(episode: SEpisode): List<Video> = throw UnsupportedOperationException()

    suspend fun getAnimeDetails(anime: SAnime): SAnime = throw UnsupportedOperationException()

    suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = throw UnsupportedOperationException()
}
