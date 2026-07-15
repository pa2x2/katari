package mihon.entry.interactions

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import eu.kanade.presentation.theme.TachiyomiTheme

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
    TachiyomiTheme {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodySmall,
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
