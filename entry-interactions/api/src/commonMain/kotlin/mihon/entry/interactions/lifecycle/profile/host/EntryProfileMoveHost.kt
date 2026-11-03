package mihon.entry.interactions.lifecycle.profile.host

import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveConflict
import mihon.entry.interactions.lifecycle.profile.EntryProfileMovePlan
import mihon.entry.interactions.lifecycle.profile.EntryProfileMovePreview
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveRequest
import tachiyomi.domain.entry.model.Entry

interface EntryProfileMoveHost {
    suspend fun selectedEntries(request: EntryProfileMoveRequest): List<Entry>

    suspend fun destinationConflicts(
        request: EntryProfileMoveRequest,
        sourceEntries: List<Entry>,
    ): List<EntryProfileMoveConflict>

    suspend fun execute(
        preview: EntryProfileMovePreview,
        plan: EntryProfileMovePlan,
        beforeCoreMutation: suspend () -> Unit,
        afterCoreMutation: suspend () -> Unit,
    ): EntryProfileMoveCommit
}

sealed interface EntryProfileMoveCommit {
    data object Applied : EntryProfileMoveCommit
    data object Conflict : EntryProfileMoveCommit
}
