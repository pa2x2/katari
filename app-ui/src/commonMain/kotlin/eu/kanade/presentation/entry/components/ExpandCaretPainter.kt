package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/** The caret of a section that unfolds, such as the description: pointing down to unfold, up to fold. */
@Composable
internal expect fun expandCaretPainter(expanded: Boolean): Painter
