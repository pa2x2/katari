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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import mihon.text.recognition.api.result.TextRecognitionResult

/** The tried image with recognized regions outlined, followed by their text in reading order. */
@Composable
fun TextRecognitionPlaygroundResult(
    image: Bitmap,
    result: TextRecognitionResult?,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    val imageBitmap = remember(image) { image.asImageBitmap() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
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
                    result.regions.forEach { region ->
                        drawRect(
                            color = color,
                            topLeft = Offset(region.bounds.left * scaleX, region.bounds.top * scaleY),
                            size = Size(region.bounds.width * scaleX, region.bounds.height * scaleY),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
        }
        result?.regions?.forEachIndexed { index, region ->
            Text(
                text = "${index + 1}. ${region.text}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
