package app.katari.entry.interactions.manga.reader.image.background

import kotlin.math.abs

/*
 * How the rules for the space beside a page read and compare colours, given as ARGB without premultiplied alpha.
 */

private val Int.red: Int get() = (this shr 16) and 0xFF
private val Int.green: Int get() = (this shr 8) and 0xFF
private val Int.blue: Int get() = this and 0xFF

internal fun Int.luma(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

// The colour most of these are near: each channel's middle value, which a few stray colours don't move
internal fun List<Int>.medianColor(): Int {
    fun median(channel: (Int) -> Int) = map(channel).sorted()[size / 2]
    return (0xFF shl 24) or (median { it.red } shl 16) or (median { it.green } shl 8) or median { it.blue }
}

private fun Int.channelDistance(other: Int): Int =
    maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

// Close enough to pass for one colour in a picture, as JPEG noise and antialiasing leave it
internal fun Int.isCloseColor(other: Int): Boolean = channelDistance(other) < CLOSE_DISTANCE

// One colour held exactly, as a gap's is and a gradient's isn't
internal fun Int.isSameColor(other: Int): Boolean = channelDistance(other) <= SAME_DISTANCE

// White, black, grey or a slight tint, as the space between panels is
internal fun Int.isNeutralColor(): Boolean = maxOf(red, green, blue) - minOf(red, green, blue) < NEUTRAL_SPREAD

private const val CLOSE_DISTANCE = 30
private const val SAME_DISTANCE = 6
private const val NEUTRAL_SPREAD = 48
