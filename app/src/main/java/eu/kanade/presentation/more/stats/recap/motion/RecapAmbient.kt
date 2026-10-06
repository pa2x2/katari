package eu.kanade.presentation.more.stats.recap.motion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Motion that keeps a page alive once its entrance ends. It follows the story's clock, so it stands at rest, at
 * phase 0, when animations are removed; every wave below is at its rest value there.
 */

/** Rises from 0 to 1 and back once every [periodMillis]. */
internal fun ambientWave(millis: Long, periodMillis: Long): Float =
    ((1.0 - cos(2.0 * PI * (millis % periodMillis) / periodMillis)) / 2.0).toFloat()

/** Swings from 0 to 1, through 0 to -1 and back once every [periodMillis]. */
internal fun ambientSwing(millis: Long, periodMillis: Long): Float =
    sin(2.0 * PI * (millis % periodMillis) / periodMillis).toFloat()

/** Lifts a cover a little and tilts it back and forth, slowly, as if it floated. */
@Composable
internal fun Modifier.recapFloat(): Modifier {
    val clock = LocalRecapClock.current
    val lift = 7.dp
    return graphicsLayer {
        val millis = clock.storyMillis
        translationY = -lift.toPx() * ambientWave(millis, FLOAT_MILLIS)
        rotationZ = TILT_DEGREES * ambientSwing(millis, FLOAT_MILLIS)
    }
}

/** Fades an element down and back up, to draw the eye to it, such as a prompt to tap. */
@Composable
internal fun Modifier.recapPulse(): Modifier {
    val clock = LocalRecapClock.current
    return graphicsLayer { alpha = 1f - PULSE_DEPTH * ambientWave(clock.storyMillis, PULSE_MILLIS) }
}

/** How long one pulse takes, for anything that pulses in step with [recapPulse]. */
internal const val PULSE_MILLIS = 1_800L

private const val FLOAT_MILLIS = 6_000L
private const val TILT_DEGREES = 1.2f
private const val PULSE_DEPTH = 0.45f
