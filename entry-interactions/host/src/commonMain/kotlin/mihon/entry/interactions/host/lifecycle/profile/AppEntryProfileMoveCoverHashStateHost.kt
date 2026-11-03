package mihon.entry.interactions.host.lifecycle.profile

import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveStateRequest
import tachiyomi.data.DatabaseHandler

class AppEntryProfileMoveCoverHashStateHost(
    private val handler: DatabaseHandler,
) : EntryProfileMoveCoverHashStateHost {
    override suspend fun move(request: EntryProfileMoveStateRequest) {
        handler.await {
            request.entryIds.forEach { entryId ->
                entry_cover_hashesQueries.moveEntryToProfile(
                    request.destinationProfileId,
                    request.sourceProfileId,
                    entryId,
                )
            }
        }
    }
}
