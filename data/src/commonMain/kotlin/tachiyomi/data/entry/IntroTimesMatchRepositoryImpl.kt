package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.IntroTimesMatch
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.domain.entry.repository.IntroTimesMatchRepository

@Inject
@SingleIn(AppScope::class)
class IntroTimesMatchRepositoryImpl(
    private val handler: DatabaseHandler,
) : IntroTimesMatchRepository {

    override suspend fun get(entryId: Long, groupKey: String?): IntroTimesMatch? {
        return handler.awaitOneOrNull {
            intro_times_matchesQueries.get(entryId, groupKey.orEmpty(), ::mapMatch)
        }
    }

    override suspend fun getPickedByEntryId(entryId: Long): List<IntroTimesMatch> {
        return handler.awaitList {
            intro_times_matchesQueries.getPickedByEntryId(entryId, ::mapMatch)
        }
    }

    override suspend fun upsert(match: IntroTimesMatch) {
        handler.await {
            intro_times_matchesQueries.upsert(
                entryId = match.entryId,
                groupKey = match.groupKey.orEmpty(),
                anilistId = match.series?.anilistId,
                malId = match.series?.malId,
                title = match.series?.title,
                year = match.series?.year?.toLong(),
                pickedByUser = match.pickedByUser,
                checkedAt = match.checkedAt,
            )
        }
    }

    private fun mapMatch(
        entryId: Long,
        groupKey: String,
        anilistId: Long?,
        malId: Long?,
        title: String?,
        year: Long?,
        pickedByUser: Boolean,
        checkedAt: Long,
    ): IntroTimesMatch {
        val series = if (anilistId != null && malId != null && title != null) {
            IntroTimesSeries(anilistId = anilistId, malId = malId, title = title, year = year?.toInt())
        } else {
            null
        }
        return IntroTimesMatch(
            entryId = entryId,
            groupKey = groupKey.ifEmpty { null },
            series = series,
            pickedByUser = pickedByUser,
            checkedAt = checkedAt,
        )
    }
}
