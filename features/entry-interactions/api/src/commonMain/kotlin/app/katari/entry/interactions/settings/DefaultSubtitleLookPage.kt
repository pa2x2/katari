package app.katari.entry.interactions.settings

import androidx.compose.runtime.Composable

/**
 * The Settings page that sets the default subtitle look, over a sample picture. The anime player owns how subtitles
 * look and are drawn, so it provides the whole page; Settings only opens it.
 */
interface DefaultSubtitleLookPage {
    @Composable
    fun Content(navigateUp: () -> Unit)
}
