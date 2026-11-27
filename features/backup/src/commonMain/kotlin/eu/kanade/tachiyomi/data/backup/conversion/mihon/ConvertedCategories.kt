package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.*

/**
 * The category lists of a Mihon-family backup as Katari's one list: Mihon's, or Aniyomi's manga and anime lists. A
 * name stands for one category, which keeps the first list's id, sort and flags (D028), and the merged list gets fresh
 * orders. Entries name their categories by order within their own list; two categories of a list can share an order
 * (TachiyomiSY writes that after restoring into a library that had categories), and since the backup can't tell which
 * of them an entry was in, the order stands for both (D060).
 */
internal class ConvertedCategories(vararg lists: List<MihonCategory>) {
    private val orders: List<Map<Long, List<Long>>>

    private val ids: List<Map<Long, Long>>

    val backupCategories: List<BackupCategory>

    val reportItems: List<Item>

    init {
        val byName = linkedMapOf<String, MihonCategory>()
        lists.forEach { list -> list.sortedBy { it.order }.forEach { byName.putIfAbsent(it.name, it) } }
        val merged = byName.values.toList()
        val firstIds = lists.first().mapTo(hashSetOf()) { it.id }
        val laterOnly = merged.toSet() - lists.first().toSet()
        var nextId = merged.maxOfOrNull { it.id }?.plus(1) ?: 0
        backupCategories = merged.mapIndexed { index, category ->
            category.toBackupCategory(order = index.toLong()).apply {
                // Ids name categories in settings, so a later list's category can't take an id of the first's
                if (category in laterOnly && id in firstIds) id = nextId++
            }
        }
        val orderByName = backupCategories.associate { it.name to it.order }
        val idByName = backupCategories.associate { it.name to it.id }
        ids = lists.map { list -> list.associate { it.id to idByName.getValue(it.name) } }
        orders = lists.map { list ->
            list.groupBy({ it.order }, { orderByName.getValue(it.name) }).mapValues { (_, merged) -> merged.distinct() }
        }
        val shared = lists.flatMap { list -> list.groupBy { it.order }.values.filter { it.size > 1 }.flatten() }
            .mapTo(hashSetOf()) { it.name }
        reportItems = merged.map { it.reportItem(sharesOrder = it.name in shared) }
    }

    /** The merged list's orders for an entry of the [list]-th list that names [entryOrders]. */
    fun of(list: Int, entryOrders: List<Long>): List<Long> =
        entryOrders.flatMap { orders[list][it].orEmpty() }.distinct()

    /** The merged list's id for the [list]-th list's category [id], as settings name it; null for an id not listed. */
    fun idOf(list: Int, id: Long): Long? = ids[list][id]
}

private fun MihonCategory.toBackupCategory(order: Long): BackupCategory = BackupCategory(
    name = name,
    order = order,
    id = id,
    flags = j2kSort?.let(::j2kLibrarySort)?.let { it.type.flag or it.direction.flag } ?: flags,
)

/**
 * The report's line for the category: entries placed in it and another for sharing its order (D060), a Komikku hidden
 * one shown in Katari (D031), a sort Katari lacks noted.
 */
private fun MihonCategory.reportItem(sharesOrder: Boolean): Item = when {
    sharesOrder -> Item(name, MR.strings.backup_conversion_category_shared_order)
    hidden -> Item(name, MR.strings.backup_conversion_hidden_category)
    j2kSort != null && j2kLibrarySort(j2kSort) == null -> Item(name, MR.strings.backup_conversion_manual_category_sort)
    else -> Item(name)
}

/**
 * TachiyomiJ2K's and Yōkai's category sort (`LibrarySort.categoryValue` in TachiyomiJ2K, Jays2Kings/tachiyomiJ2K
 * master): `'a' + 2 * n` ascending and `'b' + 2 * n` descending for its n-th sort, and `'D'` for a manual order, which
 * Katari has no counterpart for (D039).
 */
private fun j2kLibrarySort(char: Int): LibrarySort? {
    val offset = char - 'a'.code
    if (offset < 0) return null
    val type = when (offset / 2) {
        0 -> LibrarySort.Type.Alphabetical
        1 -> LibrarySort.Type.LastRead
        2 -> LibrarySort.Type.LatestChapter
        3 -> LibrarySort.Type.UnreadCount
        4 -> LibrarySort.Type.TotalChapters
        5 -> LibrarySort.Type.DateAdded
        6 -> LibrarySort.Type.ChapterFetchDate
        else -> return null
    }
    val direction = if (offset % 2 == 0) LibrarySort.Direction.Ascending else LibrarySort.Direction.Descending
    return LibrarySort(type, direction)
}
