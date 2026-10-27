package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable

/** The app graph [KatariTheme] reads the user's theme from, as each platform makes it reachable from composition. */
@Composable
internal expect fun rememberKatariThemeGraph(): KatariThemeGraph
