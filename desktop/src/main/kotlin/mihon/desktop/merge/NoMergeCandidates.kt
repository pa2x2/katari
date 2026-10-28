package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.merge.EntryMergeCandidateFeature
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

/** Desktop doesn't host merged entries yet, so no entry is offered to merge with. Stands in until it does. */
@Inject
class NoMergeCandidates : EntryMergeCandidateFeature {
    override suspend fun candidates(entry: Entry): List<DuplicateEntryCandidate> = emptyList()

    override fun observeCandidates(entry: Flow<Entry>): Flow<List<DuplicateEntryCandidate>> = entry.map { emptyList() }
}
