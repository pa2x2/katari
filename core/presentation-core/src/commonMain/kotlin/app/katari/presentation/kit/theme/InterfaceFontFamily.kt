package app.katari.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/** The family the platform sets interface text in, where the kit's default one isn't it; null keeps the default. */
@Composable
internal expect fun rememberInterfaceFontFamily(): FontFamily?
