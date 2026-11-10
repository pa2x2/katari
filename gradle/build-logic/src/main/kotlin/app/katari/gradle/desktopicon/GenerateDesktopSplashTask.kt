package app.katari.gradle.desktopicon

import com.android.ide.common.vectordrawable.VdPreview
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Draws the desktop app's icon as the PNG the Java launcher shows while the app starts, long before the app can draw
 * anything itself. The launcher reads no SVG, so this draws the layers [GenerateDesktopIconTask] writes as one, in the
 * same rounded square.
 *
 * The launcher cuts its window to the image's opaque pixels and shows each pixel whole or not at all, so the corners
 * are left transparent and the square's edge is cut hard: a smoothed edge would be thresholded on screen anyway.
 */
@CacheableTask
abstract class GenerateDesktopSplashTask : DefaultTask() {

    /** A vector drawable. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val background: RegularFileProperty

    /** A vector drawable. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val foreground: RegularFileProperty

    /** The image's side in pixels, which is its size on every screen: the launcher shows it unscaled. */
    @get:Input
    abstract val size: Property<Int>

    @get:OutputFile
    abstract val splash: RegularFileProperty

    @TaskAction
    fun generate() {
        val side = size.get()
        val image = BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val corner = side * LOGO_CORNER * 2
            graphics.clip = RoundRectangle2D.Double(0.0, 0.0, side.toDouble(), side.toDouble(), corner, corner)
            listOf(background, foreground).forEach { layer ->
                graphics.drawImage(drawLayer(layer.get().asFile, side), 0, 0, side, side, null)
            }
        } finally {
            graphics.dispose()
        }
        val splash = splash.get().asFile.apply { parentFile.mkdirs() }
        check(ImageIO.write(image, "png", splash)) { "This Java writes no PNG" }
    }

    private fun drawLayer(drawable: File, side: Int): BufferedImage {
        val errors = StringBuilder()
        val layer = VdPreview.getPreviewFromVectorXml(
            VdPreview.TargetSize.createFromMaxDimension(side),
            drawable.readText(),
            errors,
        )
        check(layer != null && errors.isEmpty()) { "${drawable.name}: $errors" }
        return layer
    }
}
