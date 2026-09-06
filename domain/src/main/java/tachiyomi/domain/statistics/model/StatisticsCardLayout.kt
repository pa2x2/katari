package tachiyomi.domain.statistics.model

/** Stable persisted identities, independent of labels and current data availability. */
enum class StatisticsCard(val id: String) {
    SUMMARY("summary"),
    ACTIVITY("activity"),
    TOP_TITLES("top_titles"),
    PATTERNS("patterns"),
    EARLIER("earlier"),
    PROGRESS("progress"),
    MEDIA("media"),
    INSIGHTS("insights"),
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
        fun decode(value: String): StatisticsCardLayout {
            val parts = value.split(';')
            fun cards(
                part: String,
            ) = part.split(',').mapNotNull { id -> StatisticsCard.entries.find { it.id == id } }.distinct()
            val saved = cards(parts.first())
            return StatisticsCardLayout(
                order = saved + StatisticsCard.entries.filterNot(saved::contains),
                hidden = parts.getOrNull(1)?.let(::cards)?.toSet().orEmpty(),
            )
        }
    }
}
