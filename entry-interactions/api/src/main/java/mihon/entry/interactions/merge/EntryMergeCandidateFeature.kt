package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

/** Merge-aware duplicate candidates without exposing the membership data used to derive them. */
interface EntryMergeCandidateFeature {
    suspend fun candidates(entry: Entry): List<DuplicateEntryCandidate>

    fun observeCandidates(entry: Flow<Entry>): Flow<List<DuplicateEntryCandidate>>
}
