package mihon.entry.interactions.manga.reader.image.background

import kotlin.math.abs

/** What the paged viewer paints around a page that doesn't fill the screen, when the background follows the page. */
internal sealed interface PageBackground {
    /** [argb] without premultiplied alpha. */
    data class Solid(val argb: Int) : PageBackground

    /** Colours spread evenly from top to bottom. */
    data class Gradient(val argb: List<Int>) : PageBackground
}

/** A page's pixels as ARGB without premultiplied alpha, read one at a time. */
internal class PagePixels(val width: Int, val height: Int, private val read: (x: Int, y: Int) -> Int) {
    operator fun get(x: Int, y: Int): Int = read(x, y)
}

/**
 * Picks the background that continues the page's own margins: the colour of a dark or coloured frame, white for a
 * white one, or a gradient when only the top or the bottom is dark. A [landscape] screen shows the page beside its
 * background rather than above and below it, so it never gets a gradient.
 */
internal fun choosePageBackground(image: PagePixels, landscape: Boolean): PageBackground {
    if (image.width < 50 || image.height < 50) {
        return PageBackground.Solid(WHITE)
    }

    val top = 5
    val bot = image.height - 5
    val left = (image.width * 0.0275).toInt()
    val right = image.width - left
    val midX = image.width / 2
    val midY = image.height / 2
    val offsetX = (image.width * 0.01).toInt()
    val leftOffsetX = left - offsetX
    val rightOffsetX = right + offsetX

    val topLeftPixel = image[left, top]
    val topRightPixel = image[right, top]
    val midLeftPixel = image[left, midY]
    val midRightPixel = image[right, midY]
    val topCenterPixel = image[midX, top]
    val botLeftPixel = image[left, bot]
    val bottomCenterPixel = image[midX, bot]
    val botRightPixel = image[right, bot]

    val topLeftIsDark = topLeftPixel.isDark()
    val topRightIsDark = topRightPixel.isDark()
    val midLeftIsDark = midLeftPixel.isDark()
    val midRightIsDark = midRightPixel.isDark()
    val topMidIsDark = topCenterPixel.isDark()
    val botLeftIsDark = botLeftPixel.isDark()
    val botRightIsDark = botRightPixel.isDark()

    var darkBG =
        (topLeftIsDark && (botLeftIsDark || botRightIsDark || topRightIsDark || midLeftIsDark || topMidIsDark)) ||
            (topRightIsDark && (botRightIsDark || botLeftIsDark || midRightIsDark || topMidIsDark))

    val topAndBotPixels =
        listOf(topLeftPixel, topCenterPixel, topRightPixel, botRightPixel, bottomCenterPixel, botLeftPixel)
    val isNotWhiteAndCloseTo = topAndBotPixels.mapIndexed { index, color ->
        val other = topAndBotPixels[(index + 1) % topAndBotPixels.size]
        !color.isWhite() && color.isCloseTo(other)
    }
    if (isNotWhiteAndCloseTo.all { it }) {
        return PageBackground.Solid(topLeftPixel)
    }

    val cornerPixels = listOf(topLeftPixel, topRightPixel, botLeftPixel, botRightPixel)
    if (cornerPixels.count { it.isWhite() } > 2) {
        darkBG = false
    }

    var blackColor = when {
        topLeftIsDark -> topLeftPixel
        topRightIsDark -> topRightPixel
        botLeftIsDark -> botLeftPixel
        botRightIsDark -> botRightPixel
        else -> WHITE
    }

    var overallWhitePixels = 0
    var overallBlackPixels = 0
    var topBlackStreak = 0
    var topWhiteStreak = 0
    var botBlackStreak = 0
    var botWhiteStreak = 0
    outer@ for (x in intArrayOf(left, right, leftOffsetX, rightOffsetX)) {
        var whitePixelsStreak = 0
        var whitePixels = 0
        var blackPixelsStreak = 0
        var blackPixels = 0
        var blackStreak = false
        var whiteStreak = false
        val notOffset = x == left || x == right
        inner@ for ((index, y) in (0..<image.height step image.height / 25).withIndex()) {
            val pixel = image[x, y]
            val pixelOff = image[x + (if (x < image.width / 2) -offsetX else offsetX), y]
            if (pixel.isWhite()) {
                whitePixelsStreak++
                whitePixels++
                if (notOffset) {
                    overallWhitePixels++
                }
                if (whitePixelsStreak > 14) {
                    whiteStreak = true
                }
                if (whitePixelsStreak > 6 && whitePixelsStreak >= index - 1) {
                    topWhiteStreak = whitePixelsStreak
                }
            } else {
                whitePixelsStreak = 0
                if (pixel.isDark() && pixelOff.isDark()) {
                    blackPixels++
                    if (notOffset) {
                        overallBlackPixels++
                    }
                    blackPixelsStreak++
                    if (blackPixelsStreak >= 14) {
                        blackStreak = true
                    }
                    continue@inner
                }
            }
            if (blackPixelsStreak > 6 && blackPixelsStreak >= index - 1) {
                topBlackStreak = blackPixelsStreak
            }
            blackPixelsStreak = 0
        }
        if (blackPixelsStreak > 6) {
            botBlackStreak = blackPixelsStreak
        } else if (whitePixelsStreak > 6) {
            botWhiteStreak = whitePixelsStreak
        }
        when {
            blackPixels > 22 -> {
                if (x == right || x == rightOffsetX) {
                    blackColor = when {
                        topRightIsDark -> topRightPixel
                        botRightIsDark -> botRightPixel
                        else -> blackColor
                    }
                }
                darkBG = true
                overallWhitePixels = 0
                break@outer
            }
            blackStreak -> {
                darkBG = true
                if (x == right || x == rightOffsetX) {
                    blackColor = when {
                        topRightIsDark -> topRightPixel
                        botRightIsDark -> botRightPixel
                        else -> blackColor
                    }
                }
                if (blackPixels > 18) {
                    overallWhitePixels = 0
                    break@outer
                }
            }
            whiteStreak || whitePixels > 22 -> darkBG = false
        }
    }

    val topIsBlackStreak = topBlackStreak > topWhiteStreak
    val bottomIsBlackStreak = botBlackStreak > botWhiteStreak
    if (overallWhitePixels > 9 && overallWhitePixels > overallBlackPixels) {
        darkBG = false
    }
    if (topIsBlackStreak && bottomIsBlackStreak) {
        darkBG = true
    }

    if (landscape) {
        return PageBackground.Solid(if (darkBG) blackColor else WHITE)
    }

    val botCornersIsWhite = botLeftPixel.isWhite() && botRightPixel.isWhite()
    val topCornersIsWhite = topLeftPixel.isWhite() && topRightPixel.isWhite()

    val topCornersIsDark = topLeftIsDark && topRightIsDark
    val botCornersIsDark = botLeftIsDark && botRightIsDark

    val topOffsetCornersIsDark = image[leftOffsetX, top].isDark() && image[rightOffsetX, top].isDark()
    val botOffsetCornersIsDark = image[leftOffsetX, bot].isDark() && image[rightOffsetX, bot].isDark()

    val darkTop = listOf(blackColor, blackColor, WHITE, WHITE)
    val darkBottom = listOf(WHITE, WHITE, blackColor, blackColor)
    return when {
        darkBG && botCornersIsWhite -> PageBackground.Gradient(darkTop)
        darkBG && topCornersIsWhite -> PageBackground.Gradient(darkBottom)
        darkBG -> PageBackground.Solid(blackColor)
        topIsBlackStreak ||
            (topCornersIsDark && topOffsetCornersIsDark && (topMidIsDark || overallBlackPixels > 9)) ->
            PageBackground.Gradient(darkTop)
        bottomIsBlackStreak ||
            (botCornersIsDark && botOffsetCornersIsDark && (bottomCenterPixel.isDark() || overallBlackPixels > 9)) ->
            PageBackground.Gradient(darkBottom)
        else -> PageBackground.Solid(WHITE)
    }
}

internal const val WHITE = -0x1

private val Int.alpha: Int get() = (this ushr 24) and 0xFF
private val Int.red: Int get() = (this shr 16) and 0xFF
private val Int.green: Int get() = (this shr 8) and 0xFF
private val Int.blue: Int get() = this and 0xFF

private fun Int.isDark(): Boolean = red < 40 && blue < 40 && green < 40 && alpha > 200

private fun Int.isCloseTo(other: Int): Boolean =
    abs(red - other.red) < 30 && abs(green - other.green) < 30 && abs(blue - other.blue) < 30

private fun Int.isWhite(): Boolean = red + blue + green > 740
