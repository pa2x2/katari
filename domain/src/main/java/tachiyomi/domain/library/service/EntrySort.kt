package tachiyomi.domain.library.service

import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.util.orderedPresentIds

fun <T> entrySortComparator(
    sorting: Long,
    sortDescending: Boolean,
    sortingSourceFlag: Long,
    sortingNumberFlag: Long,
    sortingUploadDateFlag: Long,
    sortingAlphabetFlag: Long,
    numberSelector: (T) -> Double,
    dateUploadSelector: (T) -> Long,
    nameSelector: (T) -> String,
    urlSelector: (T) -> String,
    sourceOrderSelector: (T) -> Long,
    sourceOrderNewestFirst: Boolean = true,
): Comparator<T> {
    val comparator = when (sorting) {
        sortingNumberFlag -> Comparator<T> { a, b ->
            val numA = numberSelector(a)
            val numB = numberSelector(b)
            val recognizedA = numA >= 0.0
            val recognizedB = numB >= 0.0
            if (recognizedA != recognizedB) {
                return@Comparator if (recognizedA) -1 else 1
            }

            val numCompare = if (sortDescending) {
                numB.compareTo(numA)
            } else {
                numA.compareTo(numB)
            }
            if (numCompare != 0) return@Comparator numCompare
            sourceOrderSelector(a).compareTo(sourceOrderSelector(b))
        }
        sortingUploadDateFlag -> Comparator<T> { a, b ->
            val dateA = dateUploadSelector(a)
            val dateB = dateUploadSelector(b)
            val hasDateA = dateA > 0L
            val hasDateB = dateB > 0L
            if (hasDateA != hasDateB) {
                return@Comparator if (hasDateA) -1 else 1
            }

            val dateCompare = if (sortDescending) {
                dateB.compareTo(dateA)
            } else {
                dateA.compareTo(dateB)
            }
            if (dateCompare != 0) return@Comparator dateCompare
            sourceOrderSelector(a).compareTo(sourceOrderSelector(b))
        }
        sortingAlphabetFlag -> Comparator<T> { a, b ->
            val nameA = nameSelector(a).ifBlank { urlSelector(a) }
            val nameB = nameSelector(b).ifBlank { urlSelector(b) }
            val nameCompare = nameA.compareToWithCollator(nameB)
            if (nameCompare != 0) return@Comparator nameCompare
            sourceOrderSelector(a).compareTo(sourceOrderSelector(b))
        }
        else -> {
            val baseComparator = Comparator<T> { a, b ->
                sourceOrderSelector(a).compareTo(sourceOrderSelector(b))
            }
            if (sourceOrderNewestFirst) {
                // For manga source order, "descending" means "newest first".
                // Since sourceOrder 0 is the newest item from the source,
                // descending corresponds to ascending numeric sourceOrder.
                if (sortDescending) baseComparator else baseComparator.reversed()
            } else {
                if (sortDescending) baseComparator.reversed() else baseComparator
            }
        }
    }

    return when {
        sorting == sortingSourceFlag -> comparator // direction already applied above
        sorting == sortingNumberFlag -> comparator // direction already applied above
        sorting == sortingUploadDateFlag -> comparator // direction already applied above
        sortDescending -> comparator.reversed()
        else -> comparator
    }
}

fun <T> List<T>.sortedForMergedDisplay(
    mergedIds: List<Long>,
    idSelector: (T) -> Long,
    itemComparator: Comparator<T>,
): List<T> {
    if (mergedIds.size <= 1) {
        return sortedWith(itemComparator)
    }

    return mergedIds.orderedPresentIds(this, idSelector)
        .flatMap { id ->
            asSequence()
                .filter { idSelector(it) == id }
                .sortedWith(itemComparator)
                .toList()
        }
}

fun <T> List<T>.sortedForReading(
    mergedIds: List<Long>,
    idSelector: (T) -> Long,
    itemComparator: Comparator<T>,
    sortDescending: Boolean,
): List<T> {
    if (mergedIds.size <= 1) {
        return sortedWith(itemComparator)
    }

    val orderedMergedIds = mergedIds.orderedPresentIds(this, idSelector).let { ids ->
        if (sortDescending) ids.asReversed() else ids
    }
    return orderedMergedIds.flatMap { id ->
        asSequence()
            .filter { idSelector(it) == id }
            .sortedWith(itemComparator)
            .toList()
    }
}

fun <T> List<T>.groupedByMergedMember(
    mergedIds: List<Long>,
    idSelector: (T) -> Long,
): List<Pair<Long, List<T>>> {
    return mergedIds.orderedPresentIds(this, idSelector)
        .mapNotNull { id ->
            val memberItems = filter { idSelector(it) == id }
            memberItems.takeIf { it.isNotEmpty() }?.let { id to it }
        }
}
