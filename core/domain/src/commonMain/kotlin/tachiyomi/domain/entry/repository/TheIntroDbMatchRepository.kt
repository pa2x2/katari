package tachiyomi.domain.entry.repository

import tachiyomi.domain.entry.model.TheIntroDbMatch

interface TheIntroDbMatchRepository {

    suspend fun get(entryId: Long, groupKey: String?): TheIntroDbMatch?

    suspend fun getPickedByEntryId(entryId: Long): List<TheIntroDbMatch>

    suspend fun upsert(match: TheIntroDbMatch)
}
