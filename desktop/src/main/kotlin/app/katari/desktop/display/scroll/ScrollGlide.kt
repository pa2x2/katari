package app.katari.desktop.display.scroll

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln

/**
 * How content goes on moving after a touchpad's fingers flicked it and lifted: from the speed they moved it at it
 * slows by the same share each moment, as the desktop's own scrolling views do, until it is too slow to see. Speeds
 * are the content's, in pixels a second.
 */
internal class ScrollGlide private constructor(private val velocityX: Double, private val velocityY: Double) {
    private val lastsMillis: Double = SLOWING_MS * ln(hypot(velocityX, velocityY) / END_SPEED)

    fun isOver(elapsedMillis: Double): Boolean = elapsedMillis >= lastsMillis

    /** How far the content has moved [elapsedMillis] after the fingers lifted, along each axis. */
    fun travelled(elapsedMillis: Double): Pair<Double, Double> {
        val seconds = SLOWING_MS / MILLIS * (1 - exp(-elapsedMillis.coerceAtMost(lastsMillis) / SLOWING_MS))
        return velocityX * seconds to velocityY * seconds
    }

    companion object {
        /** The glide that fingers lifting at [velocityX] and [velocityY] leave; null when they were too slow for one. */
        fun from(velocityX: Double, velocityY: Double): ScrollGlide? {
            val speed = hypot(velocityX, velocityY)
            if (speed < START_SPEED) return null
            val capped = minOf(speed, TOP_SPEED) / speed
            return ScrollGlide(velocityX * capped, velocityY * capped)
        }

        // Fingers that lift while moving the content slower than this were placing it, not flicking it
        private const val START_SPEED = 600.0
        private const val END_SPEED = 30.0
        private const val TOP_SPEED = 12_000.0

        // The glide loses 63% of its speed in this long, which makes it travel 0.3 s of the fingers' speed in all
        private const val SLOWING_MS = 300.0
        private const val MILLIS = 1_000.0
    }
}

/**
 * The speed content moves at as a touchpad's fingers lift, from the steps they moved it by just before: the distance
 * of the last [WINDOW_MS] over that time, so fingers that rested before lifting leave none.
 */
internal class FingerVelocity {
    private class Step(val timeMillis: Long, val x: Double, val y: Double)

    private val steps = ArrayDeque<Step>()
    private var startedAt = 0L

    fun start(timeMillis: Long) {
        steps.clear()
        startedAt = timeMillis
    }

    fun add(timeMillis: Long, x: Double, y: Double) {
        steps += Step(timeMillis, x, y)
        while (steps.first().timeMillis < timeMillis - WINDOW_MS) steps.removeFirst()
    }

    /** Pixels a second along each axis for fingers lifting at [timeMillis]. */
    fun atLift(timeMillis: Long): Pair<Double, Double> {
        val recent = steps.filter { it.timeMillis >= timeMillis - WINDOW_MS }
        // A scroll shorter than the window took only as long as it lasted
        val seconds = (timeMillis - startedAt).coerceIn(SHORTEST_MS, WINDOW_MS) / MILLIS
        return recent.sumOf { it.x } / seconds to recent.sumOf { it.y } / seconds
    }

    private companion object {
        const val WINDOW_MS = 80L

        // Two reports of a touchpad apart, below which a speed is noise
        const val SHORTEST_MS = 16L
        const val MILLIS = 1_000.0
    }
}
