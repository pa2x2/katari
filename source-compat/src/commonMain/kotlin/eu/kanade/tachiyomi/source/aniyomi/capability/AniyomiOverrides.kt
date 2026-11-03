package eu.kanade.tachiyomi.source.aniyomi.capability

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import java.lang.reflect.Method

/*
 * What an extension does is read from the members its classes declare, never from the extensions-lib it says it was
 * built against: extensions declare one lib and use another's symbols. Aniyomi's player decides hosters the same way.
 * Only the extension's own classes count; the base classes Katari provides declare defaults for everything.
 */

/** Lists servers before videos (extensions-lib 16 and later). */
internal val AnimeSource.hasHosters: Boolean
    get() = declaresAny("getHosterList", "hosterListRequest", "hosterListParse")

/** Finishes a video's link only once it is picked; `resolveVideo` is a member of the website base class. */
internal val AnimeSource.resolvesVideos: Boolean
    get() = this is AnimeHttpSource && declaresAny("resolveVideo")

/** How an extension lists titles related to one of its own; null when it doesn't. */
internal enum class RelatedTitlesCall {
    /** extensions-lib 17's `getRelatedAnimeList(anime)`, in named groups. */
    GROUPED,

    /** Komikku's form that pushes (label, titles) groups as they arrive. */
    PUSHED,

    /** Komikku's `fetchRelatedAnimeList`, from the site's own page for the title. */
    FETCHED,
}

/*
 * Komikku's website base class says every source supports related titles and parses them from the details page with
 * the popular-list parser, and its catalogue adds keyword searches named after words of the title. Neither is a
 * relation the site states, so only an extension that implements related titles itself is offered them, as for manga.
 */
internal val AnimeSource.relatedTitlesCall: RelatedTitlesCall?
    get() = when {
        supportsRelatedAnime -> RelatedTitlesCall.GROUPED
        this !is AnimeCatalogueSource || !supportsRelatedAnimes || disableRelatedAnimes -> null
        declaresAny("getRelatedAnimeList") {
            it.parameterCount == PUSHED_RELATED_PARAMETERS
        } -> RelatedTitlesCall.PUSHED
        declaresAny(
            "fetchRelatedAnimeList",
            "relatedAnimeListRequest",
            "relatedAnimeListParse",
            "relatedAnimeListSelector",
            "relatedAnimeFromElement",
        ) -> RelatedTitlesCall.FETCHED
        else -> null
    }

internal fun AnimeSource.declaresAny(vararg names: String, matches: (Method) -> Boolean = { true }): Boolean =
    generateSequence<Class<*>>(javaClass) { it.superclass }
        .takeWhile { it !in HOST_BASE_CLASSES }
        .any { type -> type.declaredMethods.any { it.name in names && matches(it) } }

// The title, the exception handler, the push callback and the continuation
private const val PUSHED_RELATED_PARAMETERS = 4

private val HOST_BASE_CLASSES = setOf(AnimeHttpSource::class.java, ParsedAnimeHttpSource::class.java, Any::class.java)
