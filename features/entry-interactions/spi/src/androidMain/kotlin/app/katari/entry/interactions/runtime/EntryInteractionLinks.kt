package app.katari.entry.interactions.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.katari.core.metro.metroGraph

/**
 * Opens web links from a reader's or player's composition through the app's link opener, which these activities' own
 * compositions don't get from the app's composition root.
 */
@Composable
fun rememberEntryInteractionLinkOpening(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        val linkOpener = context.metroGraph<EntryInteractionActivityGraph>().linkOpener
        { url -> linkOpener.open(context, url) }
    }
}
