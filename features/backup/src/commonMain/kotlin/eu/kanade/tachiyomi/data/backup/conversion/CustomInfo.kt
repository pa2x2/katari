package eu.kanade.tachiyomi.data.backup.conversion

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Row
import tachiyomi.i18n.*

/**
 * What a person set on an entry over its source's info, as TachiyomiSY, Komikku, TachiyomiJ2K, Yōkai and the Aniyomi
 * forks keep it, each at its own field numbers. Katari keeps only the title, as the entry's display name (D027).
 */
internal class CustomInfo(
    private val title: String?,
    private val artist: String?,
    private val author: String?,
    private val description: String?,
    private val genre: List<String>?,
    private val status: Int?,
    private val cover: String?,
) {
    val displayName: String?
        get() = title?.takeIf { it.isNotBlank() }

    val leftBehind: List<StringResource>
        get() = listOfNotNull(
            MR.strings.backup_conversion_custom_author.takeIf { author != null },
            MR.strings.backup_conversion_custom_artist.takeIf { artist != null },
            MR.strings.backup_conversion_custom_description.takeIf { description != null },
            MR.strings.backup_conversion_custom_genres.takeIf { genre != null },
            MR.strings.backup_conversion_custom_status.takeIf { status != null },
            MR.strings.backup_conversion_custom_cover.takeIf { cover != null },
        )
}

/**
 * The report's custom info row, collected while entries stream past, one entry per [K]: an entry the backup holds more
 * than once counts once (D034).
 */
internal class CustomInfoReport<K> {
    private val titles = linkedMapOf<K, String>()
    private val leftBehind = linkedMapOf<K, List<Item>>()

    /** Adds the custom info of the entry at [key], which the report calls [title]. */
    fun add(key: K, title: String, info: CustomInfo) {
        if (info.displayName != null) titles[key] = title
        info.leftBehind.takeIf { it.isNotEmpty() }?.let { notes -> leftBehind[key] = notes.map { Item(title, it) } }
    }

    fun row(): Row? = Row.of(
        Kind.CUSTOM_INFO,
        carried = titles.values.map { Item(it, MR.strings.backup_conversion_custom_title) },
        leftBehind = leftBehind.values.flatten(),
    )
}
