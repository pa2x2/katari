package app.katari.entry.interactions.media

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/** A switch a page's media adds to the immersive view's controls, such as a clip's subtitles. */
@Immutable
data class EntryImmersiveMediaSwitch(
    val label: String,
    val icon: ImageVector,
    val on: Boolean,
    val onToggle: () -> Unit,
)

/** The switches one page's media adds to the controls. Its renderer [offer]s them and offers none when it leaves. */
@Stable
class EntryImmersiveMediaSwitches {
    var offered by mutableStateOf(emptyList<EntryImmersiveMediaSwitch>())
        private set

    fun offer(switches: List<EntryImmersiveMediaSwitch>) {
        offered = switches
    }
}
