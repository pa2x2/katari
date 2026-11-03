package mihon.desktop.display.size

/**
 * The interface size, in percent of the session's scale. From half to double the session's size covers a session
 * scale set for another screen in either direction while keeping text legible and the window's layout usable, and
 * steps of a tenth are as fine as the shortcuts need while reaching any size in a few presses.
 */
object InterfaceSize {
    const val DEFAULT = 100
    const val STEP = 10
    val RANGE = 50..200

    /** The next step up from [percent], which a size between steps reaches first. */
    fun larger(percent: Int): Int = (stepAtOrBelow(percent) + STEP).coerceIn(RANGE)

    /** The next step down from [percent], which a size between steps reaches first. */
    fun smaller(percent: Int): Int = (stepAtOrAbove(percent) - STEP).coerceIn(RANGE)

    private fun stepAtOrBelow(percent: Int): Int = percent.floorDiv(STEP) * STEP

    private fun stepAtOrAbove(percent: Int): Int = -(-percent).floorDiv(STEP) * STEP
}
