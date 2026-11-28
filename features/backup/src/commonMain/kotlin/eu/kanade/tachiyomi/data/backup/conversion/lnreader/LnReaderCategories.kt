package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import tachiyomi.i18n.*

/**
 * LNReader's categories as Katari's. Its built-in Default and Local aren't carried: their novels are in no category,
 * which Katari shows as its own Default (D093). The rest keep their names and ids and are ordered as LNReader sorts
 * them; two can share a sort, and then the older one comes first.
 */
internal class LnReaderCategories(categories: List<LnReaderCategory>) {
    private val builtIn = categories.filter { it.id in BUILT_IN_IDS }

    private val own = categories - builtIn.toSet()

    val backupCategories: List<BackupCategory> = own.sortedWith(compareBy({ it.sort }, { it.id }))
        .mapIndexed { index, category ->
            BackupCategory(name = category.name, order = index.toLong(), id = category.id)
        }

    private val orderByNovel: Map<Long, List<Long>> = buildMap<Long, MutableList<Long>> {
        val orderById = backupCategories.associate { it.id to it.order }
        own.forEach { category ->
            category.novelIds.forEach { novel -> getOrPut(novel, ::mutableListOf) += orderById.getValue(category.id) }
        }
    }

    val reportItems: List<Item> = backupCategories.map { Item(it.name) } +
        builtIn.map { Item(it.name, MR.strings.backup_conversion_built_in_category) }

    /** The orders of the categories LNReader's novel [novelId] is in, as Katari's entries name them. */
    fun of(novelId: Long): List<Long> = orderByNovel[novelId].orEmpty()

    private companion object {
        // LNReader's BUILT_IN_CATEGORY_IDS, Default and Local (src/database/constants.ts)
        val BUILT_IN_IDS = setOf(1L, 2L)
    }
}
