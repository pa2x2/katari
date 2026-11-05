package mihon.entry.interactions.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mihon.core.metro.metroGraph
import mihon.core.platform.clipboard.TextClipboard

/** The app's clipboard for a reader's or player's composition, which these activities' own compositions don't get. */
@Composable
fun rememberEntryInteractionTextClipboard(): TextClipboard {
    val context = LocalContext.current
    return remember(context) { context.metroGraph<EntryInteractionActivityGraph>().textClipboard }
}
