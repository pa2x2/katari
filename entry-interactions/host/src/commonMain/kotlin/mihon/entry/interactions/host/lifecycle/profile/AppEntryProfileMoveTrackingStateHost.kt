package mihon.entry.interactions.host.lifecycle.profile

import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveStateRequest
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveTrackingStateHost
import tachiyomi.data.DatabaseHandler

class AppEntryProfileMoveTrackingStateHost(
    private val handler: DatabaseHandler,
) : EntryProfileMoveTrackingStateHost {
    override suspend fun move(request: EntryProfileMoveStateRequest) {
        handler.await {
            request.entryIds.forEach { entryId ->
                entry_syncQueries.moveEntryToProfile(request.destinationProfileId, request.sourceProfileId, entryId)
            }
        }
    }
}
