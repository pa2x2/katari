package app.katari.entry.interactions.runtime

import androidx.compose.runtime.Composable

/**
 * The host app's root of the compositions of the readers and players it opens in activities of their own, which gives
 * them what its own screens get: its theme, ViewModels and links, and the marks its settings rows show.
 */
fun interface EntryInteractionCompositionRoot {
    @Composable
    fun Content(content: @Composable () -> Unit)
}
