package app.katari.tts.provider.speechdispatcher.engine

import app.katari.tts.api.provider.TtsParameterRange
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Speech Dispatcher takes rate and pitch as a step from the voice's own, -100 to 100, which each module stretches over
 * what its synthesizer can do. Katari's factors map onto it on a doubling scale: half maps to -100, double to 100, so
 * a factor and its inverse sit the same distance either side of the voice's own.
 */
internal val SPEECH_DISPATCHER_PARAMETER_RANGE = TtsParameterRange(minimum = 0.5f, maximum = 2f, default = 1f)

internal fun speechDispatcherStep(factor: Float): Int {
    val clamped = factor.coerceIn(SPEECH_DISPATCHER_PARAMETER_RANGE.minimum, SPEECH_DISPATCHER_PARAMETER_RANGE.maximum)
    return (log2(clamped) * MAXIMUM_STEP).roundToInt()
}

private const val MAXIMUM_STEP = 100
