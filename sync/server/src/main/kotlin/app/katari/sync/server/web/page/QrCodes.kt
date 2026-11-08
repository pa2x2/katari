package app.katari.sync.server.web.page

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.SVG
import kotlinx.html.svg
import kotlinx.html.visit

/**
 * [text] as a QR code a phone's camera reads from the screen, encoded as the app encodes its link codes (the UI kit's
 * QrCode). Dark on a light tile in either colour scheme, since phone scanners expect that contrast, with the quiet zone
 * around it that scanners need. Inline SVG, so it needs no image address, which the pages' content policy would refuse
 * for anything but the server's own files.
 */
internal fun FlowContent.qrCode(text: String, label: String) {
    val matrix = QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0,
        ),
    )
    // One rectangle per run of dark modules in a row, inside the quiet zone.
    val modules = buildString {
        for (y in 0..<matrix.height) {
            var x = 0
            while (x < matrix.width) {
                if (!matrix[x, y]) {
                    x++
                    continue
                }
                val start = x
                while (x < matrix.width && matrix[x, y]) x++
                append("M${start + QUIET_ZONE_MODULES} ${y + QUIET_ZONE_MODULES}h${x - start}v1h-${x - start}z")
            }
        }
    }
    val tile = matrix.width + 2 * QUIET_ZONE_MODULES
    svg("qr") {
        attributes["viewBox"] = "0 0 $tile $tile"
        attributes["role"] = "img"
        attributes["aria-label"] = label
        attributes["shape-rendering"] = "crispEdges"
        shape("rect", "width" to "100%", "height" to "100%", "rx" to "2", "fill" to "#fff")
        shape("path", "d" to modules, "fill" to "#000")
    }
}

/**
 * An SVG shape, which kotlinx.html has no builder for. Closed with an end tag: HTML parses SVG as foreign content,
 * where a start tag without `/>` or an end tag stays open and would take the next shape inside it, unseen.
 */
private fun SVG.shape(name: String, vararg attributes: Pair<String, String>) =
    HTMLTag(name, consumer, attributes.toMap(), namespace = null, inlineTag = false, emptyTag = false).visit {}

private const val QUIET_ZONE_MODULES = 4
