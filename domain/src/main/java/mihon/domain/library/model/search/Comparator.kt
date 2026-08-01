package mihon.domain.library.model.search

enum class Comparator(val symbol: String) {
    GTE(">="),
    LTE("<="),
    GT(">"),
    LT("<"),
    EQ("="),
    ;

    fun <T : Comparable<T>> apply(a: T, b: T): Boolean = when (this) {
        GTE -> a >= b
        LTE -> a <= b
        GT -> a > b
        LT -> a < b
        EQ -> a == b
    }

    companion object {
        private val lookup = entries.associateBy { it.symbol }

        fun fromString(value: String): Comparator? = lookup[value]
    }
}
