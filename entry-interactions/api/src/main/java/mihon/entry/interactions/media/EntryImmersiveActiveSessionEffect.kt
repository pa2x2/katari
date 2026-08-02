package mihon.entry.interactions.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun EntryImmersiveActiveSessionEffect(
    active: Boolean,
    onPagingBlockedChange: (Boolean) -> Unit,
    onDeactivated: () -> Unit,
) {
    val latestOnPagingBlockedChange = rememberUpdatedState(onPagingBlockedChange)
    val latestOnDeactivated = rememberUpdatedState(onDeactivated)
    DisposableEffect(active) {
        onDispose {
            if (active) {
                latestOnDeactivated.value()
                latestOnPagingBlockedChange.value(false)
            }
        }
    }
}
