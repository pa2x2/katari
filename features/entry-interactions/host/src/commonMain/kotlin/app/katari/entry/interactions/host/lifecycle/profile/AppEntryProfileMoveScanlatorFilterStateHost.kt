package app.katari.entry.interactions.host.lifecycle.profile

import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveScanlatorFilterStateHost
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveStateRequest
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
