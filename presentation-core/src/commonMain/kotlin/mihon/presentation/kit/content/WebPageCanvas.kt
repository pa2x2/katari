package mihon.presentation.kit.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Paints what a browser shows behind a web page where the page sets no background of its own: the web's canvas, white
 * whatever the app's theme. For pages drawn from pictures that leave those areas transparent, as an off-screen
 * browser's are; the page goes over it.
 */
fun DrawScope.drawWebPageCanvas() {
    drawRect(Color.White)
}
