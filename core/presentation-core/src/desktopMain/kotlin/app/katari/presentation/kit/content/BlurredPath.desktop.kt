package app.katari.presentation.kit.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint

internal actual fun DrawScope.drawBlurredPath(path: Path, color: Color, cssBlur: Float) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, cssBlur / 2)
        }
        canvas.skiaCanvas.drawPath(path.asSkiaPath(), paint)
    }
}
