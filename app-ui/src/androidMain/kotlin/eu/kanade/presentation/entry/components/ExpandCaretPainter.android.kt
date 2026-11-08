package eu.kanade.presentation.entry.components

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.katari.ui.R

// The caret flips from one direction to the other.
@Composable
internal actual fun expandCaretPainter(expanded: Boolean): Painter {
    val image = AnimatedImageVector.animatedVectorResource(R.drawable.animated_caret_flip)
    return rememberAnimatedVectorPainter(image, !expanded)
}
