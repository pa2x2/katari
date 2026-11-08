package app.katari.presentation.kit.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.theme.KatariTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * [text] as a QR code another device's camera reads, such as a link code. It is dark on a light tile in either theme,
 * since phone scanners expect that contrast, with the quiet zone around it that scanners need.
 */
@Composable
fun QrCode(text: String, contentDescription: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) { encode(text) }
    Box(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .background(QR_LIGHT, KatariTheme.shapes.medium)
            .padding(QUIET_ZONE),
    ) {
        Canvas(Modifier.size(QR_SIZE)) {
            val module = size.width / matrix.width
            for (y in 0..<matrix.height) {
                for (x in 0..<matrix.width) {
                    if (matrix[x, y]) {
                        drawRect(
                            color = QR_DARK,
                            topLeft = Offset(x * module, y * module),
                            // A hair over a module, so no seam shows between neighbours at fractional sizes
                            size = Size(module + 0.5f, module + 0.5f),
                        )
                    }
                }
            }
        }
    }
}

private fun encode(text: String): BitMatrix = QRCodeWriter().encode(
    text,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        // The tile's padding is the quiet zone
        EncodeHintType.MARGIN to 0,
    ),
)

private val QR_SIZE = 200.dp
private val QUIET_ZONE = 16.dp
private val QR_DARK = Color(0xFF000000)
private val QR_LIGHT = Color(0xFFFFFFFF)
