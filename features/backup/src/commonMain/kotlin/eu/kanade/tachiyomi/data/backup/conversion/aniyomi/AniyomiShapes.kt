package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.mihon.MIHON_IGNORED
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonMerges
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonTracking
import eu.kanade.tachiyomi.data.backup.conversion.newer.MessageShape
import kotlinx.serialization.KSerializer

/*
 * What Katari knows of the Aniyomi family's messages beyond the fields it reads: the fields COVERAGE.md marks ignored,
 * by message, as well as the Mihon family's for manga, and trackers, which are left behind whole. A field outside these
 * and the models is a newer app's (D123).
 */

/** An anime, read as its full model and as the seasons and merges read it before converting. */
internal fun aniyomiAnimeShape() = MessageShape(
    readAs = listOf(
        AniyomiAnime.serializer().descriptor,
        AniyomiSeasonInfo.serializer().descriptor,
        MihonMerges.ENTRY_DESCRIPTOR,
    ),
    ignored = ANIYOMI_IGNORED,
    notRead = setOf(MihonTracking.serializer().descriptor),
)

/** The backup of either layout without its entries and extension APKs; [layoutFields] are the layout's own marks. */
internal fun backupShape(serializer: KSerializer<*>, vararg layoutFields: Int) = MessageShape(
    readAs = listOf(serializer.descriptor),
    // Broken sources at 100 and 102
    ignored = ANIYOMI_IGNORED + (serializer.descriptor to setOf(100, 102) + layoutFields.toSet()),
    // Custom buttons are left behind whole, named by their name alone
    notRead = setOf(AniyomiCustomButton.serializer().descriptor),
)

private val ANIYOMI_IGNORED = MIHON_IGNORED + mapOf(
    // lastModifiedAt and version at 106 and 109; a broken field at 501; the season number at 505 (D059)
    AniyomiAnime.serializer().descriptor to setOf(106, 109, 501, 505),
    // lastModifiedAt and version at 11 and 12
    AniyomiEpisode.serializer().descriptor to setOf(11, 12),
)
