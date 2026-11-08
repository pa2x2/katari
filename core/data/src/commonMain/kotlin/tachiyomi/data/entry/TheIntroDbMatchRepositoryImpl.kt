package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.Theintrodb_matches
import tachiyomi.domain.entry.model.TheIntroDbMatch
import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import tachiyomi.domain.entry.repository.TheIntroDbMatchRepository

@Inject
@SingleIn(AppScope::class)
class TheIntroDbMatchRepositoryImpl(
    private val handler: DatabaseHandler,
) : TheIntroDbMatchRepository {

    override suspend fun get(entryId: Long, groupKey: String?): TheIntroDbMatch? {
        return handler.awaitOneOrNull { theintrodb_matchesQueries.get(entryId, groupKey.orEmpty()) }?.toMatch()
    }

    override suspend fun getPickedByEntryId(entryId: Long): List<TheIntroDbMatch> {
        return handler.awaitList { theintrodb_matchesQueries.getPickedByEntryId(entryId) }.map { it.toMatch() }
    }

    override suspend fun upsert(match: TheIntroDbMatch) {
        val media = match.media
        handler.await {
            theintrodb_matchesQueries.upsert(
                entryId = match.entryId,
                groupKey = match.groupKey.orEmpty(),
                tvdbId = media?.tvdbId,
                tmdbId = media?.tmdbId,
                imdbId = media?.imdbId,
                title = media?.title,
                year = media?.year?.toLong(),
                season = media?.placement?.season?.toLong(),
                firstEpisode = media?.placement?.firstEpisode?.toLong(),
                pickedByUser = match.pickedByUser,
                checkedAt = match.checkedAt,
            )
        }
    }

    private fun Theintrodb_matches.toMatch(): TheIntroDbMatch {
        val placement = if (season != null && first_episode != null) {
            TheIntroDbPlacement(season = season.toInt(), firstEpisode = first_episode.toInt())
        } else {
            null
        }
        return TheIntroDbMatch(
            entryId = entry_id,
            groupKey = group_key.ifEmpty { null },
            media = title?.let {
                TheIntroDbMedia(
                    tvdbId = tvdb_id,
                    tmdbId = tmdb_id,
                    imdbId = imdb_id,
                    title = it,
                    year = year?.toInt(),
                    placement = placement,
                )
            },
            pickedByUser = picked_by_user,
            checkedAt = checked_at,
        )
    }
}
