package tachiyomi.domain.statistics.model

/** Stable persisted identities, independent of labels and current data availability. */
enum class StatisticsCard(val id: String) {
    ACTIVITY("activity"),
    TOP_TITLES("top_titles"),
    PATTERNS("patterns"),
    EARLIER("earlier"),
    CALENDAR("calendar"),
    LIBRARY("library"),
    INSIGHTS("insights"),
    ;

    companion object {
        /**
         * Cards that were merged into another, by the id layouts saved before the merge: the period's tiles into
         * its chart, and the progress and per-type cards into one library card.
         */
        private val mergedIds = mapOf(
            "summary" to ACTIVITY,
            "progress" to LIBRARY,
            "media" to LIBRARY,
        )

        internal fun fromId(id: String): StatisticsCard? = entries.find { it.id == id } ?: mergedIds[id]
    }
}

data class StatisticsCardLayout(
    val order: List<StatisticsCard> = StatisticsCard.entries,
    val hidden: Set<StatisticsCard> = emptySet(),
) {
    fun encode(): String = order.joinToString(",") { it.id } + ";" + hidden.joinToString(",") { it.id }

    fun move(card: StatisticsCard, offset: Int): StatisticsCardLayout {
        val items = order.toMutableList()
        val from = items.indexOf(card)
        if (from < 0) return this
        items.add((from + offset).coerceIn(items.indices), items.removeAt(from))
        return copy(order = items)
    }

    companion object {
        /**
         * A card merged from several keeps the place of the first of them and stays shown unless every one of them
         * was hidden, so nothing that was on the page goes missing.
         */
        fun decode(value: String): StatisticsCardLayout {
            val parts = value.split(';')
            val savedIds = parts.first().split(',')
            val hiddenIds = parts.getOrNull(1)?.split(',').orEmpty().toSet()
            val saved = savedIds.mapNotNull(StatisticsCard::fromId).distinct()
            return StatisticsCardLayout(
                order = saved + StatisticsCard.entries.filterNot(saved::contains),
                hidden = StatisticsCard.entries.filterTo(mutableSetOf()) { card ->
                    val ids = (savedIds + hiddenIds).filter { StatisticsCard.fromId(it) == card }
                    ids.isNotEmpty() && ids.all(hiddenIds::contains)
                },
            )
        }
    }
}
