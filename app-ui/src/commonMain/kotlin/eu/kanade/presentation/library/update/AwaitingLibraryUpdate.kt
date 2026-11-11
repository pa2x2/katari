package eu.kanade.presentation.library.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.domain.library.update.model.LibraryUpdateState
import kotlin.time.Duration.Companion.seconds

/**
 * Pull-to-refresh state for starting a library update: set it once the update is started or queued, and it clears
 * when the update shows up, handing over to the screen's progress, or when the update never shows up.
 */
@Composable
fun rememberAwaitingUpdateState(updateState: LibraryUpdateState?): MutableState<Boolean> {
    val awaitingUpdate = remember { mutableStateOf(false) }
    val currentUpdateState by rememberUpdatedState(updateState)
    LaunchedEffect(awaitingUpdate.value) {
        if (!awaitingUpdate.value) return@LaunchedEffect
        withTimeoutOrNull(UpdateStartTimeout) {
            snapshotFlow { currentUpdateState }.first { it != null }
        }
        awaitingUpdate.value = false
    }
    return awaitingUpdate
}

private val UpdateStartTimeout = 10.seconds
