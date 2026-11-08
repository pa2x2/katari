package app.katari.video.engine.spi.player

/**
 * A subtitle on screen now, which the app draws: [text], or for a subtitle made of pictures (PGS, VobSub, DVB) the
 * [image] alone. Where it goes is the subtitle file's, as shares of the area subtitles are drawn in, and every place
 * left null is the app's: a cue without [line] rests at the bottom, above the cues before it, and one without
 * [position] is centred.
 *
 * An image cue always has its [line] as a [SubtitleLine.Share], its [position] and its [width].
 */
data class SubtitleCue(
    val text: String,
    val textAlignment: SubtitleTextAlignment = SubtitleTextAlignment.Centre,
    val line: SubtitleLine? = null,
    /** Where the cue sits across: a share of the area's width, from its left edge, and which side of the cue is there. */
    val position: SubtitleAnchored? = null,
    /** The cue's width as a share of the area's; null fits it to its text. */
    val width: Float? = null,
    val image: SubtitleImage? = null,
)

enum class SubtitleTextAlignment { Start, Centre, End }

/** Where a cue sits down the area. */
sealed interface SubtitleLine {
    /** A share of the area's height, from its top edge, and which side of the cue is there. */
    data class Share(val anchored: SubtitleAnchored) : SubtitleLine

    /** A row counted in the cue's line heights from the top of the area, as CEA-608 captions place theirs. */
    data class Row(val row: Float) : SubtitleLine
}

/** A share of the area's size along one side, and which of the cue's edges, or its middle, is there. */
data class SubtitleAnchored(val share: Float, val anchor: Anchor) {
    enum class Anchor { Start, Middle, End }
}

/**
 * A cue's picture, as non-premultiplied ARGB [pixels] row by row, [width] by [height]. [heightShare] is its height as a
 * share of the area's; null keeps the picture's own shape at the cue's width.
 */
class SubtitleImage(
    val pixels: IntArray,
    val width: Int,
    val height: Int,
    val heightShare: Float? = null,
)
