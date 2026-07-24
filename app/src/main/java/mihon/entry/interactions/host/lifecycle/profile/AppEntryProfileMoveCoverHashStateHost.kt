package mihon.entry.interactions.host.lifecycle.profile

import mihon.entry.interactions.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.EntryProfileMoveStateRequest
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
