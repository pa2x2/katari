package app.katari.entry.interactions.anime.introtimes.aniskip

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The seasons of a show on AniList, which keeps each as an anime of its own, linked to the one before and after it.
 * Films and specials between seasons are followed through but aren't seasons.
 */
@Inject
@SingleIn(AppScope::class)
internal class AniListSeasons(private val search: AniListSeriesSearch) {

    // Kept for this run of the app under every season's id, so stepping through a show's episodes asks once.
    private val known = object : LinkedHashMap<Long, List<AniListSeries>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, List<AniListSeries>>) = size > MAX_KNOWN
    }

    /** The seasons the anime [anilistId] is one of; empty when AniList doesn't have it. */
    suspend fun of(anilistId: Long): List<AniListSeries> =
        synchronized(known) { known[anilistId] }
            ?: search.byIds(listOf(anilistId)).firstOrNull()?.let { of(it) }.orEmpty()

    /** The seasons [series] is one of, first to last. It is among them, alone when nothing is linked to it. */
    suspend fun of(series: AniListSeries): List<AniListSeries> {
        synchronized(known) { known[series.anilistId] }?.let { return it }
        val seen = hashSetOf(series.anilistId)
        val before = ArrayDeque<AniListSeries>()
        val after = mutableListOf<AniListSeries>()
        var back = series.prequels.next(seen)
        var forward = series.sequels.next(seen)
        while ((back != null || forward != null) && seen.size < MAX_LINKED) {
            val linked = search.byIds(listOfNotNull(back, forward)).associateBy { it.anilistId }
            back = back?.let(linked::get)?.also { before.addFirst(it) }?.prequels?.next(seen)
            forward = forward?.let(linked::get)?.also { after += it }?.sequels?.next(seen)
        }
        val seasons = (before + series + after).filter { it.isSeries || it.anilistId == series.anilistId }
        synchronized(known) { seasons.forEach { known[it.anilistId] = seasons } }
        return seasons
    }

    private companion object {
        const val MAX_KNOWN = 200
        const val MAX_LINKED = 24
    }
}

/**
 * The link to follow out of several, marked as seen: a series before a film or special, as a show's seasons continue
 * each other while its side stories branch off, and the oldest of those.
 */
private fun List<AniListLink>.next(seen: MutableSet<Long>): Long? =
    filter { it.anilistId !in seen }
        .minWithOrNull(compareBy<AniListLink> { !it.isSeries }.thenBy { it.anilistId })
        ?.anilistId
        ?.also(seen::add)

/**
 * [found] with each show's seasons brought together in the order they aired, at the place of the one AniList ranked
 * first, which returns them in an order of its own.
 */
internal fun seasonsTogether(found: List<AniListSeries>): List<AniListSeries> {
    val byId = found.associateBy { it.anilistId }
    val listed = HashSet<Long>()
    return found.flatMap { series ->
        if (!listed.add(series.anilistId)) return@flatMap emptyList()
        val show = mutableListOf(series)
        var index = 0
        while (index < show.size) {
            (show[index].prequels + show[index].sequels)
                .mapNotNull { byId[it.anilistId] }
                .filter { listed.add(it.anilistId) }
                .let(show::addAll)
            index++
        }
        show.sortedBy { if (it.startOrder > 0) it.startOrder else Int.MAX_VALUE }
    }
}
