package tachiyomi.domain.entry.interactor

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import tachiyomi.domain.entry.model.Entry

internal data class LibraryFavoriteSetKey(
    val profileId: Long,
    val orderedEntryIds: List<Long>,
)

internal sealed interface LibraryFavoriteSetObservation {
    data object Empty : LibraryFavoriteSetObservation

    data class Populated(
        val key: LibraryFavoriteSetKey,
        val updates: Channel<List<Entry>>,
    ) : LibraryFavoriteSetObservation
}

internal fun observeLibraryFavoriteSets(
    entries: Flow<List<Entry>>,
    expectedProfileId: Long?,
): Flow<LibraryFavoriteSetObservation> = flow {
    var active: LibraryFavoriteSetObservation.Populated? = null
    try {
        entries.collect { favorites ->
            if (favorites.isEmpty()) {
                emit(LibraryFavoriteSetObservation.Empty)
                active?.updates?.close()
                active = null
                return@collect
            }

            val profileId = favorites.first().profileId
            check(expectedProfileId == null || profileId == expectedProfileId) {
                "Library entries belong to profile $profileId instead of $expectedProfileId"
            }
            check(favorites.all { entry -> entry.profileId == profileId }) {
                "Library entries cannot cross profiles"
            }
            val key = LibraryFavoriteSetKey(profileId, favorites.map(Entry::id))
            val current = active
            if (current?.key == key) {
                current.updates.send(favorites)
            } else {
                val updates = Channel<List<Entry>>(Channel.CONFLATED)
                updates.send(favorites)
                val next = LibraryFavoriteSetObservation.Populated(key, updates)
                active = next
                try {
                    emit(next)
                } finally {
                    current?.updates?.close()
                }
            }
        }
    } finally {
        active?.updates?.close()
    }
}
