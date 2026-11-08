package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp

// Android flips the caret with its own animated vector, which Compose Multiplatform can't load; desktop swaps it.
@Composable
internal actual fun expandCaretPainter(expanded: Boolean): Painter = rememberVectorPainter(
    if (expanded) MaterialSymbols.Rounded.KeyboardArrowUp else MaterialSymbols.Rounded.KeyboardArrowDown,
)
