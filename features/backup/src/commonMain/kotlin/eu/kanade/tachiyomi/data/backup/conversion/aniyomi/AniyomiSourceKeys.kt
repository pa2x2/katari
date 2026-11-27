package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.withTachiyomiIdentity
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.domain.source.identity.SourceIdScheme

/**
 * The keys a converted backup refers to its sources by. Manga sources are Tachiyomi's and keep their ids as keys. An
 * anime source's id is only unique among Aniyomi's extensions and can repeat a manga source's, so anime sources are
 * listed under the `aniyomi` scheme and get a key of their own where their id is taken. Both apps' local sources are
 * Katari's one local source.
 *
 * @param mangaSourceIds every manga source the backup lists or an entry uses.
 * @param animeSources every anime source the backup lists or an entry uses, by id, with the name the backup gives.
 */
internal class AniyomiSourceKeys(
    mangaSources: List<BackupSource>,
    mangaSourceIds: Set<Long>,
    animeSources: Map<Long, String>,
) {
    private val animeKeys: Map<Long, Long>

    /** Every source with its identity, as the Katari backup lists them. */
    val backupSources: List<BackupSource>

    init {
        val taken = (mangaSourceIds + animeSources.keys).toHashSet()
        var nextKey = -1L
        animeKeys = animeSources.keys.associateWith { id ->
            when {
                id == LOCAL_SOURCE_ID -> LOCAL_SOURCE_ID
                id !in mangaSourceIds -> id
                else -> {
                    while (nextKey in taken) nextKey--
                    nextKey.also(taken::add)
                }
            }
        }
        val manga = mangaSources.map { it.withTachiyomiIdentity() }
        val anime = animeSources.filterKeys { it != LOCAL_SOURCE_ID }.map { (id, name) ->
            BackupSource(
                name = name,
                sourceId = animeKey(id),
                idScheme = SourceIdScheme.ANIYOMI.key,
                externalId = id,
            )
        }
        backupSources = manga + anime
    }

    fun animeKey(id: Long): Long = animeKeys.getValue(id)
}
