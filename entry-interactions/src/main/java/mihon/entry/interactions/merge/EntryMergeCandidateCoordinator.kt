package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.host.EntryMergeHost
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

internal class EntryMergeCandidateCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeCandidateFeature {
    override suspend fun candidates(entry: Entry): List<DuplicateEntryCandidate> {
        return host.profile(entry.profileId).duplicateCandidates(entry)
    }

    override fun observeCandidates(entry: Flow<Entry>): Flow<List<DuplicateEntryCandidate>> {
        return entry.flatMapLatest { current ->
            host.profile(current.profileId).observeDuplicateCandidates(flowOf(current))
        }
    }
}
