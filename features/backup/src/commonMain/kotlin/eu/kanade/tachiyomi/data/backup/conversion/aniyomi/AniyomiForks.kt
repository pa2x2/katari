package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents

/**
 * The fork of the Aniyomi family whose own parts a backup has (D109, research/10 section 3), or null when it has none
 * or those of more than one. Each sign is a field only that fork writes, so it shows only where the person used what it
 * holds. The anime part decides: Aniyomi's manga entries carry fields Anikku's anime entries use for its own.
 */
internal fun aniyomiFork(contents: BackupFileContents.Protobuf): String? {
    val root = contents.fieldNumbers
    val current = IS_LEGACY in root
    val inner = contents.innerFieldNumbers
    val anime = inner[if (current) ANIME else LEGACY_ANIME].orEmpty()
    val animeCategories = inner[if (current) ANIME_CATEGORIES else LEGACY_ANIME_CATEGORIES].orEmpty()
    val forks = buildSet {
        // Saved searches and feeds, hidden categories, merges, custom covers and excluded scanlators
        if (root.any { it in ANIKKU_ROOT } || ANIKKU_HIDDEN in animeCategories || anime.any { it in ANIKKU_ANIME }) {
            add(ANIKKU)
        }
        // Custom info and excluded scanlators at 200-206, next update at 113
        if (anime.any { it in ANIMIRU_ANIME }) add(ANIMIRU)
        // Manga stores at 106 as store messages, where Aniyomi writes repos; audio language at 1000
        val mangaStores = inner[MANGA_STORES].orEmpty()
        if ((current && STORE_IS_LEGACY in mangaStores) || ANIMETAIL_AUDIO_LANGUAGE in anime) add(ANIMETAIL)
    }
    return forks.singleOrNull()
}

private const val IS_LEGACY = 500
private const val ANIME = 501
private const val ANIME_CATEGORIES = 502
private const val LEGACY_ANIME = 3
private const val LEGACY_ANIME_CATEGORIES = 4
private const val MANGA_STORES = 106

private const val ANIKKU = "Anikku"
private val ANIKKU_ROOT = setOf(600, 610)
private const val ANIKKU_HIDDEN = 900
private val ANIKKU_ANIME = setOf(108, 600, 603)

private const val ANIMIRU = "Animiru"
private val ANIMIRU_ANIME = (200..206).toSet() + 113

private const val ANIMETAIL = "Animetail"
private const val STORE_IS_LEGACY = 7
private const val ANIMETAIL_AUDIO_LANGUAGE = 1000
