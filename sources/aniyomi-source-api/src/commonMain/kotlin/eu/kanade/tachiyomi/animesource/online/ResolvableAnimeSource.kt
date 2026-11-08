package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode

/** A source that can open its own site's links. */
interface ResolvableAnimeSource : AnimeSource {

    fun getUriType(uri: String): UriType

    suspend fun getAnime(uri: String): SAnime?

    suspend fun getEpisode(uri: String): SEpisode?
}
