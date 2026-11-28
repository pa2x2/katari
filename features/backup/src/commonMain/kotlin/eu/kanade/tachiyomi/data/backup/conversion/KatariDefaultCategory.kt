package eu.kanade.tachiyomi.data.backup.conversion

import app.katari.core.platform.PlatformContext
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Note
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/**
 * Katari's own category, which holds the titles in no other and shows as [name] in the person's language. Another
 * app's category of that name would be a second tab named the same, so it comes over as [renamed] (D135).
 */
internal class KatariDefaultCategory(private val name: String, private val renamed: String) {
    /** The name the other app's category [category] comes over with. */
    fun carried(category: String): String = if (category == name) renamed else category

    /** The report's note on the other app's category [category], when it comes over renamed. */
    fun note(category: String): Note? = if (category == name) {
        Note(MR.strings.backup_conversion_category_renamed, listOf(renamed, name))
    } else {
        null
    }

    companion object {
        /** As converting [app]'s backup names it. */
        fun of(context: PlatformContext, app: String): KatariDefaultCategory {
            val name = context.stringResource(MR.strings.label_default)
            return KatariDefaultCategory(
                name = name,
                renamed = context.stringResource(MR.strings.backup_conversion_category_renamed_name, name, app),
            )
        }
    }
}
