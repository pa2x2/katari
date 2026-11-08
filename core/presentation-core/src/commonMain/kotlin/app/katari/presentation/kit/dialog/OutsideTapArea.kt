package app.katari.presentation.kit.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The window of a dialog or sheet that places its own panel, behind that panel: a tap on it lands outside the panel and
 * calls [onTap] while [enabled]. The panel's surface takes the taps that land on it, so they never reach this area.
 *
 * It sits behind the panel rather than around it. Around it, the panel would need a clickable of its own to keep its
 * taps, and a clickable merges what it holds into one node, so assistive tools would read the panel's whole content as
 * a single item.
 */
@Composable
internal fun BoxScope.OutsideTapArea(enabled: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .clickable(interactionSource = null, indication = null, enabled = enabled, onClick = onTap),
    )
}
