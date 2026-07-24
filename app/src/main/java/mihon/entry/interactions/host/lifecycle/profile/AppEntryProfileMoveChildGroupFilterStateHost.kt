package mihon.entry.interactions.host.lifecycle.profile

import mihon.entry.interactions.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.EntryProfileMoveStateRequest
import tachiyomi.data.DatabaseHandler

class AppEntryProfileMoveChildGroupFilterStateHost(
    private val handler: DatabaseHandler,
) : EntryProfileMoveChildGroupFilterStateHost {
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
