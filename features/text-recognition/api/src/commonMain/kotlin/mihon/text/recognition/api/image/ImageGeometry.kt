package mihon.text.recognition.api.image

data class ImageSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0 && height > 0)
    }

    val bounds: ImageRect
        get() = ImageRect(0, 0, width, height)
}

/** A half-open pixel rectangle: [left, right) × [top, bottom). */
data class ImageRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(right > left && bottom > top) { "Empty image rectangle: $this" }
    }

    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    val area: Long
        get() = width.toLong() * height

    fun contains(x: Int, y: Int): Boolean = x in left until right && y in top until bottom

    fun intersect(other: ImageRect): ImageRect? {
        val l = maxOf(left, other.left)
        val t = maxOf(top, other.top)
        val r = minOf(right, other.right)
        val b = minOf(bottom, other.bottom)
        return if (r > l && b > t) ImageRect(l, t, r, b) else null
    }

    fun union(other: ImageRect): ImageRect = ImageRect(
        minOf(left, other.left),
        minOf(top, other.top),
        maxOf(right, other.right),
        maxOf(bottom, other.bottom),
    )
}
