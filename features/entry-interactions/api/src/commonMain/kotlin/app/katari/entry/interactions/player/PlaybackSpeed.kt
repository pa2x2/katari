package app.katari.entry.interactions.player

import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

/**
 * A speed the anime player plays at, in hundredths of normal speed: 115 plays at 1.15x. The player's settings, its
 * keys and the default speed setting share one range and one step, so a speed set in one of them is always one the
 * others can show and step from.
 */
@JvmInline
value class PlaybackSpeed(val hundredths: Int) {
    /** What the engine is told: 1.15 for 115. */
    val multiplier: Float get() = hundredths / 100f

    /** The speed as it is typed, without trailing zeros: "1", "0.75", "1.2". */
    val number: String
        get() {
            val whole = hundredths / 100
            val fraction = hundredths % 100
            return when {
                fraction == 0 -> "$whole"
                fraction % 10 == 0 -> "$whole.${fraction / 10}"
                else -> "$whole.${fraction.toString().padStart(2, '0')}"
            }
        }

    /** As the player shows it: "1x", "0.75x", "1.2x". */
    val label: String get() = "${number}x"

    /** [steps] steps faster, or slower when negative, kept within [RANGE]. */
    fun stepped(steps: Int): PlaybackSpeed = PlaybackSpeed((hundredths + steps * STEP).coerceIn(RANGE))

    /** What holding the video plays at: twice this speed, up to the fastest. */
    val held: PlaybackSpeed get() = PlaybackSpeed((hundredths * 2).coerceAtMost(RANGE.last))

    companion object {
        val NORMAL = PlaybackSpeed(100)

        /** From 0.25x to 4x, in hundredths. */
        val RANGE = 25..400

        /** 0.05x, in hundredths; every speed the player plays at is a whole number of steps. */
        const val STEP = 5

        val PRESETS = listOf(50, 75, 100, 125, 150, 200).map(::PlaybackSpeed)

        fun isValid(hundredths: Int): Boolean = hundredths in RANGE && hundredths % STEP == 0

        /** The speed on a step nearest [hundredths], kept within [RANGE]. */
        fun nearest(hundredths: Int): PlaybackSpeed =
            PlaybackSpeed(((hundredths.toFloat() / STEP).roundToInt() * STEP).coerceIn(RANGE))

        /**
         * A typed speed, such as "1.15", "1,15" or "1.15x", put on the nearest step; null when the text isn't a number
         * or the number is outside [RANGE].
         */
        fun parse(text: String): PlaybackSpeed? {
            val number = text.trim().removeSuffix("x").removeSuffix("×").trim().replace(',', '.')
            if (number.isEmpty() || number.any { !it.isDigit() && it != '.' }) return null
            val hundredths = number.toDoubleOrNull()?.let { (it * 100).roundToInt() } ?: return null
            return if (hundredths in RANGE) nearest(hundredths) else null
        }
    }
}
