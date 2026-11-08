package app.katari.presentation.kit.theme

import androidx.compose.runtime.Composable

actual interface PlatformKatariThemeGraph : KatariThemeGraph {
    val systemColorScheme: SystemColorScheme
    val systemAccentColor: SystemAccentColor
    val systemInterfaceFont: SystemInterfaceFont
}

@Composable
internal actual fun rememberKatariThemeGraph(): PlatformKatariThemeGraph =
    checkNotNull(LocalKatariThemeGraph.current) { "No KatariThemeGraph provided around this window's content" }
