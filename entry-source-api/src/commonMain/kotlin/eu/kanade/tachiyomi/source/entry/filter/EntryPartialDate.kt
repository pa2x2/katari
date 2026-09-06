package eu.kanade.tachiyomi.source.entry.filter

/** A calendar date whose omitted components remain omitted in persistence and provider requests. */
data class EntryPartialDate(val year: Int, val month: Int? = null, val day: Int? = null) {
    init {
        require(year in 1..9999) { "Year must be between 1 and 9999" }
        require(month == null || month in 1..12) { "Month must be between 1 and 12" }
        require(day == null || (month != null && day in 1..daysInMonth(year, month))) { "Invalid calendar day" }
    }

    val precision: EntryDatePrecision
        get() = when {
            day != null -> EntryDatePrecision.DAY
            month != null -> EntryDatePrecision.MONTH
            else -> EntryDatePrecision.YEAR
        }

    /** Comparison endpoints only; these do not change the supplied precision. */
    fun earliestDayKey(): Int = year * 10000 + (month ?: 1) * 100 + (day ?: 1)
    fun latestDayKey(): Int = year * 10000 + (month ?: 12) * 100 + (day ?: daysInMonth(year, month ?: 12))

    override fun toString(): String = buildString {
        append(year.toString().padStart(4, '0'))
        month?.let { append('-').append(it.toString().padStart(2, '0')) }
        day?.let { append('-').append(it.toString().padStart(2, '0')) }
    }

    companion object {
        private val pattern = Regex("(\\d{4})(?:-(\\d{1,2})(?:-(\\d{1,2}))?)?")

        /** Returns null for malformed or impossible dates; blank input is handled by the owning filter. */
        fun parse(value: String): EntryPartialDate? {
            val match = pattern.matchEntire(value.trim()) ?: return null
            return runCatching {
                EntryPartialDate(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toIntOrNull(),
                    match.groupValues[3].toIntOrNull(),
                )
            }.getOrNull()
        }

        fun daysInMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}

enum class EntryDatePrecision { YEAR, MONTH, DAY }
