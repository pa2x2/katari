package app.katari.text.recognition.provider.onnx.paddle.lines

import kotlin.math.abs

/** The projective transform that maps four points onto four others, as `Matrix.setPolyToPoly` makes one. */
internal class PerspectiveTransform private constructor(private val h: DoubleArray) {

    /** Maps ([x], [y]) into [result] as x, y, which callers reuse to spare an allocation per pixel. */
    fun map(x: Double, y: Double, result: DoubleArray) {
        val w = h[6] * x + h[7] * y + 1.0
        result[0] = (h[0] * x + h[1] * y + h[2]) / w
        result[1] = (h[3] * x + h[4] * y + h[5]) / w
    }

    companion object {
        /**
         * The transform that maps each of the four points in [from] onto the matching point in [to], both given as
         * x, y pairs; null when three of the points lie on a line, which no such transform maps.
         */
        fun between(from: DoubleArray, to: DoubleArray): PerspectiveTransform? {
            require(from.size == 8 && to.size == 8)
            // Each pair gives two linear equations in the eight unknowns, with the ninth coefficient fixed at 1.
            val rows = Array(8) { DoubleArray(9) }
            for (point in 0 until 4) {
                val x = from[2 * point]
                val y = from[2 * point + 1]
                val u = to[2 * point]
                val v = to[2 * point + 1]
                rows[2 * point] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -x * u, -y * u, u)
                rows[2 * point + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -x * v, -y * v, v)
            }
            return solve(rows)?.let(::PerspectiveTransform)
        }

        /** Gaussian elimination with partial pivoting of an augmented 8×9 system. */
        private fun solve(rows: Array<DoubleArray>): DoubleArray? {
            val size = rows.size
            for (column in 0 until size) {
                val pivot = (column until size).maxBy { abs(rows[it][column]) }
                if (abs(rows[pivot][column]) < SINGULAR) return null
                rows[column] = rows[pivot].also { rows[pivot] = rows[column] }
                for (row in column + 1 until size) {
                    val factor = rows[row][column] / rows[column][column]
                    for (k in column..size) rows[row][k] -= factor * rows[column][k]
                }
            }
            val solution = DoubleArray(size)
            for (row in size - 1 downTo 0) {
                var sum = rows[row][size]
                for (k in row + 1 until size) sum -= rows[row][k] * solution[k]
                solution[row] = sum / rows[row][row]
            }
            return solution
        }

        private const val SINGULAR = 1e-12
    }
}
