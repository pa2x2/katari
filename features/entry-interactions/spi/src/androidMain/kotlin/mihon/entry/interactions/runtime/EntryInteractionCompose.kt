package mihon.entry.interactions.runtime

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.surface.ContentOn
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** Hosts processor-owned Compose UI with the application's active theme and content defaults. */
fun ComponentActivity.setEntryInteractionContent(content: @Composable () -> Unit) {
    setContent {
        EntryInteractionTheme(content)
    }
}

/** Hosts processor-owned Compose UI embedded in a View-based reader. */
fun ComposeView.setEntryInteractionContent(content: @Composable () -> Unit) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        EntryInteractionTheme(content)
    }
}

/** Applies the application's active Compose theme to a processor-owned composition. */
@Composable
fun EntryInteractionTheme(content: @Composable () -> Unit) {
    KatariTheme {
        ContentOn(SurfaceContainer.Background) {
            ProvideTextRole(TextRole.Supporting, content)
        }
    }
}
