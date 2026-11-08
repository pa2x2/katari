package tachiyomi.domain.entry.repository

import tachiyomi.domain.entry.model.IntroTimesMatch

interface IntroTimesMatchRepository {

    suspend fun get(entryId: Long, groupKey: String?): IntroTimesMatch?

    suspend fun getPickedByEntryId(entryId: Long): List<IntroTimesMatch>

    suspend fun upsert(match: IntroTimesMatch)
}
