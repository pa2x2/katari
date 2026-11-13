package app.katari.entry.interactions.runtime

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import app.katari.core.metro.metroGraph
import app.katari.presentation.kit.content.ProvideTextRole
import app.katari.presentation.kit.surface.ContentOn
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/** Hosts processor-owned Compose UI under the application's root ([EntryInteractionCompositionRoot]). */
fun ComponentActivity.setEntryInteractionContent(content: @Composable () -> Unit) {
    val root = metroGraph<EntryInteractionActivityGraph>().entryInteractionCompositionRoot
    setContent {
        root.Content(content)
    }
}

/** Hosts processor-owned Compose UI embedded in a View-based reader, under the application's root. */
fun ComposeView.setEntryInteractionContent(content: @Composable () -> Unit) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    val root = context.metroGraph<EntryInteractionActivityGraph>().entryInteractionCompositionRoot
    setContent {
        root.Content(content)
    }
}

/**
 * Applies the application's active Compose theme to a processor-owned composition that shows no settings, such as a
 * reader's pages.
 */
@Composable
fun EntryInteractionTheme(content: @Composable () -> Unit) {
    KatariTheme {
        ContentOn(SurfaceContainer.Background) {
            ProvideTextRole(TextRole.Supporting, content)
        }
    }
}
