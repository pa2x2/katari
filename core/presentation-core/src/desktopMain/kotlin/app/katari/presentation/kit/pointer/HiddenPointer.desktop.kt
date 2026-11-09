package app.katari.presentation.kit.pointer

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

actual val HiddenPointer: PointerIcon by lazy {
    val blank = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    PointerIcon(Toolkit.getDefaultToolkit().createCustomCursor(blank, Point(0, 0), "hidden"))
}
