package eu.kanade.tachiyomi.ui.library.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.update.model.LibraryUpdateState

/** The library update of whichever profile is active, following profile switches. */
@OptIn(ExperimentalCoroutinesApi::class)
fun LibraryUpdateQueue.activeProfileState(activeProfileProvider: ActiveProfileProvider): Flow<LibraryUpdateState?> {
    return activeProfileProvider.activeProfileIdFlow.flatMapLatest { stateFlow(it) }
}
