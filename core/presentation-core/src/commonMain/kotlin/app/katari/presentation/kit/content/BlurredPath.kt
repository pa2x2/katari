package app.katari.presentation.kit.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Fills [path] blurred as CSS blurs a shadow, where [cssBlur] is twice the Gaussian deviation. */
internal expect fun DrawScope.drawBlurredPath(path: Path, color: Color, cssBlur: Float)
