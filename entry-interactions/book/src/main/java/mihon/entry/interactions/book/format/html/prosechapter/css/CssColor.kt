package mihon.entry.interactions.book.format.html.prosechapter.css

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Parses a CSS colour into ARGB: hex (3, 4, 6 or 8 digits), `rgb()`/`rgba()` and `hsl()`/`hsla()` in comma
 * and space syntax, `hwb()`, every named colour and `transparent`. `currentcolor` and anything else return null,
 * leaving the colour to what the text inherits.
 */
internal fun parseCssColor(raw: String?): Long? {
    val value = raw?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return null
    CSS_NAMED_COLORS[value]?.let { return it }
    if (value.startsWith('#')) return parseHexColor(value.drop(1))
    val function = value.substringBefore('(', "").trim()
    if (function.isEmpty() || !value.endsWith(')')) return null
    val arguments = value.substringAfter('(').dropLast(1).cssColorArguments() ?: return null
    return when (function) {
        "rgb", "rgba" -> rgbColor(arguments)
        "hsl", "hsla" -> hslColor(arguments)
        "hwb" -> hwbColor(arguments)
        else -> null
    }
}

private fun parseHexColor(digits: String): Long? {
    if (digits.any { it.digitToIntOrNull(16) == null }) return null
    fun expand(index: Int) = digits[index].digitToInt(16) * 17L
    return when (digits.length) {
        3 -> argb(0xFF, expand(0), expand(1), expand(2))
        4 -> argb(expand(3), expand(0), expand(1), expand(2))
        6 -> 0xFF000000L or digits.toLong(16)
        8 -> (digits.substring(6).toLong(16) shl 24) or digits.substring(0, 6).toLong(16)
        else -> null
    }
}

// Splits "255, 0, 0, 0.5" and "255 0 0 / 50%" alike into channel values and an optional alpha.
private fun String.cssColorArguments(): ColorArguments? {
    val (channels, alpha) = if ('/' in this) substringBefore('/') to substringAfter('/') else this to null
    val parts = channels.split(',', ' ', '\t', '\n').map(String::trim).filter(String::isNotEmpty)
    return when {
        alpha != null && parts.size == 3 -> ColorArguments(parts, alpha.trim())
        alpha == null && parts.size == 3 -> ColorArguments(parts, null)
        alpha == null && parts.size == 4 -> ColorArguments(parts.take(3), parts[3])
        else -> null
    }
}

private class ColorArguments(val channels: List<String>, val alpha: String?)

private fun rgbColor(arguments: ColorArguments): Long? {
    val channels = arguments.channels.map { channel ->
        when {
            channel.endsWith('%') -> channel.dropLast(1).toFloatOrNull()?.times(2.55f)
            else -> channel.toFloatOrNull()
        }?.roundToInt()?.coerceIn(0, 255)?.toLong() ?: return null
    }
    return argb(arguments.alphaByte() ?: return null, channels[0], channels[1], channels[2])
}

private fun hslColor(arguments: ColorArguments): Long? {
    val hue = arguments.channels[0].toHue() ?: return null
    val saturation = arguments.channels[1].toUnitFraction() ?: return null
    val lightness = arguments.channels[2].toUnitFraction() ?: return null
    val (red, green, blue) = hslToRgb(hue, saturation, lightness)
    return argb(arguments.alphaByte() ?: return null, red, green, blue)
}

private fun hwbColor(arguments: ColorArguments): Long? {
    val hue = arguments.channels[0].toHue() ?: return null
    var white = arguments.channels[1].toUnitFraction() ?: return null
    var black = arguments.channels[2].toUnitFraction() ?: return null
    if (white + black > 1f) {
        val sum = white + black
        white /= sum
        black /= sum
    }
    val (red, green, blue) = hslToRgb(hue, 1f, 0.5f).toList().map { channel ->
        ((channel / 255f) * (1f - white - black) + white).times(255f).roundToLong().coerceIn(0, 255)
    }
    return argb(arguments.alphaByte() ?: return null, red, green, blue)
}

private fun hslToRgb(hue: Float, saturation: Float, lightness: Float): Triple<Long, Long, Long> {
    fun channel(offset: Float): Long {
        val k = (offset + hue / 30f) % 12f
        val amount = saturation * minOf(lightness, 1f - lightness)
        val value = lightness - amount * maxOf(-1f, minOf(k - 3f, 9f - k, 1f))
        return (value * 255f).roundToLong().coerceIn(0, 255)
    }
    return Triple(channel(0f), channel(8f), channel(4f))
}

private fun String.toHue(): Float? {
    val degrees = when {
        endsWith("deg") -> removeSuffix("deg").toFloatOrNull()
        endsWith("grad") -> removeSuffix("grad").toFloatOrNull()?.times(0.9f)
        endsWith("rad") -> removeSuffix("rad").toFloatOrNull()?.times(180f / Math.PI.toFloat())
        endsWith("turn") -> removeSuffix("turn").toFloatOrNull()?.times(360f)
        else -> toFloatOrNull()
    } ?: return null
    return ((degrees % 360f) + 360f) % 360f
}

private fun String.toUnitFraction(): Float? =
    (if (endsWith('%')) dropLast(1).toFloatOrNull() else toFloatOrNull())?.div(100f)?.coerceIn(0f, 1f)

private fun ColorArguments.alphaByte(): Long? {
    val value = alpha ?: return 0xFF
    val fraction = if (value.endsWith('%')) value.dropLast(1).toFloatOrNull()?.div(100f) else value.toFloatOrNull()
    return fraction?.coerceIn(0f, 1f)?.times(255f)?.roundToLong()
}

private fun argb(alpha: Long, red: Long, green: Long, blue: Long): Long =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
