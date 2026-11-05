package tachiyomi.core.common.util.system

/** One half of a double-page spread. */
enum class SpreadSide {
    RIGHT,
    LEFT,
    ;

    val opposite: SpreadSide
        get() = when (this) {
            RIGHT -> LEFT
            LEFT -> RIGHT
        }
}
