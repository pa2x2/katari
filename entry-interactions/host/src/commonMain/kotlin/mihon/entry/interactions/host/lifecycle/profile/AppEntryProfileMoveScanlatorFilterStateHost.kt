package mihon.entry.interactions.host.lifecycle.profile

import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveScanlatorFilterStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveStateRequest
import tachiyomi.data.DatabaseHandler

class AppEntryProfileMoveScanlatorFilterStateHost(
    private val handler: DatabaseHandler,
) : EntryProfileMoveScanlatorFilterStateHost {
    override suspend fun move(request: EntryProfileMoveStateRequest) {
        handler.await {
            request.entryIds.forEach { entryId ->
                excluded_scanlatorsQueries.moveEntryToProfile(
                    request.destinationProfileId,
                    request.sourceProfileId,
                    entryId,
                )
            }
        }
    }
}
