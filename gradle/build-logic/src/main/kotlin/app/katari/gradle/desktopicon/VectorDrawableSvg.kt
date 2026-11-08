package app.katari.gradle.desktopicon

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.math.BigDecimal
import javax.xml.parsers.DocumentBuilderFactory

/**
 * An Android vector drawable read as SVG: [content] is its groups and paths as SVG elements in its viewport's
 * coordinates, and [width] its size in dp. Only what launcher icon layers are drawn with is read (groups with their
 * transforms, paths with their fill and stroke); any other element or attribute fails the build rather than drawing
 * something other than what the Android app shows.
 */
internal class VectorDrawableSvg(
    val width: Double,
    val height: Double,
    val viewportWidth: Double,
    val viewportHeight: Double,
    val content: String,
)

internal fun readVectorDrawable(file: File): VectorDrawableSvg {
    val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    val root = factory.newDocumentBuilder().parse(file).documentElement
    val reader = DrawableReader(file)
    reader.check(root, "vector", VECTOR_ATTRIBUTES)
    return VectorDrawableSvg(
        width = reader.dp(root, "width"),
        height = reader.dp(root, "height"),
        viewportWidth = reader.number(root, "viewportWidth"),
        viewportHeight = reader.number(root, "viewportHeight"),
        content = buildString { reader.appendChildren(root, this) },
    )
}

private class DrawableReader(private val file: File) {
    fun appendChildren(parent: Element, svg: StringBuilder) {
        val children = parent.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            when (child.nodeType) {
                Node.ELEMENT_NODE -> appendElement(child as Element, svg)
                Node.TEXT_NODE -> check(child.textContent.isBlank()) { "${file.name}: text in <${parent.localName}>" }
            }
        }
    }

    private fun appendElement(element: Element, svg: StringBuilder) {
        when (element.localName) {
            "group" -> {
                check(element, "group", GROUP_ATTRIBUTES)
                val transform = groupTransform(element)
                svg.append(if (transform == null) "<g>" else "<g transform=\"$transform\">")
                appendChildren(element, svg)
                svg.append("</g>")
            }
            "path" -> {
                check(element, "path", PATH_ATTRIBUTES)
                svg.append("<path d=\"").append(escaped(required(element, "pathData"))).append('"')
                appendPaint(element, svg, "fill", colorAttribute = "fillColor", alphaAttribute = "fillAlpha")
                if (attribute(element, "fillType") == "evenOdd") svg.append(" fill-rule=\"evenodd\"")
                appendStroke(element, svg)
                svg.append("/>")
            }
            else -> error("${file.name}: <${element.localName}> isn't drawn by the desktop icon")
        }
    }

    // Android applies a group's transforms as scale, then rotation, both about the pivot, then translation
    private fun groupTransform(group: Element): String? {
        val translateX = optionalNumber(group, "translateX") ?: 0.0
        val translateY = optionalNumber(group, "translateY") ?: 0.0
        val scaleX = optionalNumber(group, "scaleX") ?: 1.0
        val scaleY = optionalNumber(group, "scaleY") ?: 1.0
        val rotation = optionalNumber(group, "rotation") ?: 0.0
        val pivotX = optionalNumber(group, "pivotX") ?: 0.0
        val pivotY = optionalNumber(group, "pivotY") ?: 0.0
        val parts = buildList {
            if (translateX + pivotX != 0.0 || translateY + pivotY != 0.0) {
                add("translate(${svgNumber(translateX + pivotX)} ${svgNumber(translateY + pivotY)})")
            }
            if (rotation != 0.0) add("rotate(${svgNumber(rotation)})")
            if (scaleX != 1.0 || scaleY != 1.0) add("scale(${svgNumber(scaleX)} ${svgNumber(scaleY)})")
            if (pivotX != 0.0 || pivotY != 0.0) add("translate(${svgNumber(-pivotX)} ${svgNumber(-pivotY)})")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun appendStroke(path: Element, svg: StringBuilder) {
        val width = optionalNumber(path, "strokeWidth") ?: 0.0
        if (width == 0.0 || attribute(path, "strokeColor") == null) return
        appendPaint(path, svg, "stroke", colorAttribute = "strokeColor", alphaAttribute = "strokeAlpha")
        svg.append(" stroke-width=\"${svgNumber(width)}\"")
        attribute(path, "strokeLineCap")?.let { svg.append(" stroke-linecap=\"${escaped(it)}\"") }
        attribute(path, "strokeLineJoin")?.let { svg.append(" stroke-linejoin=\"${escaped(it)}\"") }
        optionalNumber(path, "strokeMiterLimit")?.let { svg.append(" stroke-miterlimit=\"${svgNumber(it)}\"") }
    }

    // A path without a colour isn't filled on Android, where SVG fills it black
    private fun appendPaint(
        path: Element,
        svg: StringBuilder,
        paint: String,
        colorAttribute: String,
        alphaAttribute: String,
    ) {
        val color = attribute(path, colorAttribute)
        if (color == null) {
            if (paint == "fill") svg.append(" fill=\"none\"")
            return
        }
        val (rgb, colorAlpha) = parseColor(color)
        svg.append(" $paint=\"$rgb\"")
        val opacity = colorAlpha * (optionalNumber(path, alphaAttribute) ?: 1.0)
        if (opacity != 1.0) svg.append(" $paint-opacity=\"${svgNumber(opacity)}\"")
    }

    // #RGB, #ARGB, #RRGGBB or #AARRGGBB; a colour resource has no value the build can read here
    private fun parseColor(value: String): Pair<String, Double> {
        val digits = value.removePrefix("#")
        check(value.startsWith("#") && digits.length in setOf(3, 4, 6, 8) && digits.all { it.isHexDigit() }) {
            "${file.name}: colour $value isn't a literal #RGB, #ARGB, #RRGGBB or #AARRGGBB"
        }
        val full = if (digits.length <= 4) digits.flatMap { listOf(it, it) }.joinToString("") else digits
        val alpha = if (full.length == 8) full.take(2).toInt(16) / 255.0 else 1.0
        return "#${full.takeLast(6).lowercase()}" to alpha
    }

    fun check(element: Element, name: String, allowed: Set<String>) {
        check(element.localName == name) { "${file.name}: <${element.localName}> where <$name> was expected" }
        val attributes = element.attributes
        for (index in 0 until attributes.length) {
            val attribute = attributes.item(index)
            if (attribute.namespaceURI == XMLNS_NAMESPACE) continue
            check(attribute.namespaceURI == ANDROID_NAMESPACE && attribute.localName in allowed) {
                "${file.name}: <$name ${attribute.nodeName}> isn't drawn by the desktop icon"
            }
        }
    }

    fun dp(element: Element, name: String): Double {
        val value = required(element, name)
        check(value.endsWith("dp")) { "${file.name}: $name $value isn't in dp" }
        return value.removeSuffix("dp").toDouble()
    }

    fun number(element: Element, name: String): Double = required(element, name).toDouble()

    private fun optionalNumber(element: Element, name: String): Double? = attribute(element, name)?.toDouble()

    private fun required(element: Element, name: String): String =
        checkNotNull(attribute(element, name)) { "${file.name}: <${element.localName}> has no android:$name" }

    private fun attribute(element: Element, name: String): String? =
        element.getAttributeNodeNS(ANDROID_NAMESPACE, name)?.value
}

internal fun svgNumber(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

private fun escaped(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/"

private val VECTOR_ATTRIBUTES = setOf("width", "height", "viewportWidth", "viewportHeight")
private val GROUP_ATTRIBUTES =
    setOf("name", "translateX", "translateY", "scaleX", "scaleY", "rotation", "pivotX", "pivotY")
private val PATH_ATTRIBUTES = setOf(
    "name",
    "pathData",
    "fillColor",
    "fillAlpha",
    "fillType",
    "strokeColor",
    "strokeWidth",
    "strokeAlpha",
    "strokeLineCap",
    "strokeLineJoin",
    "strokeMiterLimit",
)
