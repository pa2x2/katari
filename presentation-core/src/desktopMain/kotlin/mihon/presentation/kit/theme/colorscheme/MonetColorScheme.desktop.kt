package mihon.presentation.kit.theme.colorscheme

import androidx.compose.runtime.Composable

// Desktop has no system colour source yet, so Dynamic looks like the default theme, as on Android before 8.1
@Composable
internal actual fun rememberMonetColorScheme(): () -> BaseColorScheme = { TachiyomiColorScheme }
