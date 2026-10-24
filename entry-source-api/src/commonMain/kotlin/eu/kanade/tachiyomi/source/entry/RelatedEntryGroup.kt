package eu.kanade.tachiyomi.source.entry

/**
 * Entries related to another in one way, such as its sequels or what readers also liked.
 *
 * @property title what relates the entries, shown as the group's heading in the source's own words; null when the source
 * doesn't split its related entries up, in which case it returns this group alone and the host lists it plainly.
 * @property entries the group's entries in display order.
 */
data class RelatedEntryGroup(
    val title: String?,
    val entries: List<SEntry>,
)
