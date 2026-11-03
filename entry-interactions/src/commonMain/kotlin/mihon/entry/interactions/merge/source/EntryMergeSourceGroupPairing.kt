package mihon.entry.interactions.merge.source

import tachiyomi.domain.entry.model.Entry

/**
 * Pairs two merged entries' locked members, each in source order: by position when both list as many, since sources
 * often name members alike ("Season 1"), otherwise by title. A member without a pair is left out.
 */
internal fun pairSourceGroupMembers(current: List<Entry>, replacement: List<Entry>): List<Pair<Entry, Entry>> {
    if (current.size == replacement.size) return current.zip(replacement)
    val replacementByTitle = replacement.groupBy { it.title.pairingKey() }.mapValues { it.value.toMutableList() }
    return current.mapNotNull { member ->
        replacementByTitle[member.title.pairingKey()]?.removeFirstOrNull()?.let { member to it }
    }
}

private fun String.pairingKey(): String = trim().lowercase()
