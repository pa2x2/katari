package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonCategory
import eu.kanade.tachiyomi.data.backup.conversion.mihon.reportItem
import eu.kanade.tachiyomi.data.backup.conversion.mihon.toBackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategory

/**
 * Aniyomi's manga and anime category lists as Katari's one list, where a name stands for one category: a name
 * in both lists is one category, which keeps the manga one's id, sort and flags (D028). Entries name their categories
 * by order within their own list, so each list's orders map onto the merged list's.
 */
internal class AniyomiCategories(manga: List<MihonCategory>, anime: List<MihonCategory>) {
    private val mangaOrders: Map<Long, Long>
    private val animeOrders: Map<Long, Long>

    val backupCategories: List<BackupCategory>

    val reportItems: List<Item>

    init {
        val byName = linkedMapOf<String, MihonCategory>()
        (manga.sortedBy { it.order } + anime.sortedBy { it.order }).forEach { byName.putIfAbsent(it.name, it) }
        val merged = byName.values.toList()
        val mangaIds = manga.mapTo(hashSetOf()) { it.id }
        val animeOnly = merged.toSet() - manga.toSet()
        var nextId = merged.maxOfOrNull { it.id }?.plus(1) ?: 0
        backupCategories = merged.mapIndexed { index, category ->
            category.toBackupCategory().apply {
                order = index.toLong()
                // Ids name categories in settings, so an anime category's id can't be a manga category's
                if (category in animeOnly && id in mangaIds) id = nextId++
            }
        }
        reportItems = merged.map { it.reportItem() }
        val orderByName = backupCategories.associate { it.name to it.order }
        mangaOrders = manga.associate { it.order to orderByName.getValue(it.name) }
        animeOrders = anime.associate { it.order to orderByName.getValue(it.name) }
    }

    fun ofManga(orders: List<Long>): List<Long> = orders.mapNotNull(mangaOrders::get).distinct()

    fun ofAnime(orders: List<Long>): List<Long> = orders.mapNotNull(animeOrders::get).distinct()
}
