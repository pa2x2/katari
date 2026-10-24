package eu.kanade.tachiyomi.source.aniyomi.capability

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource

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

internal fun AnimeSource.declaresAny(vararg names: String): Boolean =
    generateSequence<Class<*>>(javaClass) { it.superclass }
        .takeWhile { it !in HOST_BASE_CLASSES }
        .any { type -> type.declaredMethods.any { it.name in names } }

private val HOST_BASE_CLASSES = setOf(AnimeHttpSource::class.java, ParsedAnimeHttpSource::class.java, Any::class.java)
