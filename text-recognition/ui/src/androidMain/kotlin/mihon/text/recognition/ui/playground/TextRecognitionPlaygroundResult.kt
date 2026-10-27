package mihon.text.recognition.ui.playground

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.drawingStyle
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.window.currentWindowLayout
import mihon.text.recognition.api.result.TextRecognitionResult

/**
 * The tried image with recognized regions outlined and numbered, followed by their text in reading order under the
 * same numbers.
 */
@Composable
fun TextRecognitionPlaygroundResult(
    image: Bitmap,
    result: TextRecognitionResult?,
    modifier: Modifier = Modifier,
) {
    val color = KatariTheme.markup.mark
    val badgeTextColor = KatariTheme.markup.onMark
    val badgeText = TextRole.Badge.drawingStyle
    val outlineWidth = KatariTheme.sizes.selectedBorder
    val badgeRadius = KatariTheme.sizes.iconSmall / 2
    val maxImageHeight = currentWindowLayout().size.height * MAX_IMAGE_WINDOW_FRACTION
    val textMeasurer = rememberTextMeasurer()
    val imageBitmap = remember(image) { image.asImageBitmap() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxImageHeight)
                .aspectRatio(image.width.toFloat() / image.height, matchHeightConstraintsFirst = true),
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )
            if (result != null) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val scaleX = size.width / result.imageSize.width
                    val scaleY = size.height / result.imageSize.height
                    val badgeRadiusPx = badgeRadius.toPx()
                    result.regions.forEachIndexed { index, region ->
                        val topLeft = Offset(region.bounds.left * scaleX, region.bounds.top * scaleY)
                        drawRect(
                            color = color,
                            topLeft = topLeft,
                            size = Size(region.bounds.width * scaleX, region.bounds.height * scaleY),
                            style = Stroke(width = outlineWidth.toPx()),
                        )
                        // Kept whole inside the image even for regions at its edges.
                        val center = Offset(
                            topLeft.x.coerceIn(badgeRadiusPx, size.width - badgeRadiusPx),
                            topLeft.y.coerceIn(badgeRadiusPx, size.height - badgeRadiusPx),
                        )
                        drawCircle(color = color, radius = badgeRadiusPx, center = center)
                        val number = textMeasurer.measure((index + 1).toString(), badgeText)
                        drawText(
                            textLayoutResult = number,
                            color = badgeTextColor,
                            topLeft = center - Offset(number.size.width / 2f, number.size.height / 2f),
                        )
                    }
                }
            }
        }
        result?.regions?.forEachIndexed { index, region ->
            Text(text = "${index + 1}. ${region.text}", role = TextRole.Body)
        }
    }
}

// Tall pages such as webtoon strips stay small enough to see whole, with their recognized text below.
private const val MAX_IMAGE_WINDOW_FRACTION = 0.6f
