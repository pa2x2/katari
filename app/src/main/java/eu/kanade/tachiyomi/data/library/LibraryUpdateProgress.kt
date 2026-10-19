package eu.kanade.tachiyomi.data.library

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/** How far a running library update is; [total] stays 0 until the entries to update are known. */
data class LibraryUpdateProgress(
    val scope: LibraryUpdateScope,
    /** Names of the category, source and type the update is limited to; null when it isn't limited to any. */
    val scopeName: String?,
    val completed: Int,
    val total: Int,
    /** Entries in scope that the skip rules left out. */
    val skipped: Int,
) {
    /** "Updating Reading · 12 / 37 · 3 skipped by rules", worded the same in the library and the shade. */
    fun label(context: Context): String {
        val scopeLabel = scopeName?.let { context.stringResource(MR.strings.updating_named, it) }
            ?: context.stringResource(
                when (scope) {
                    LibraryUpdateScope.Library -> MR.strings.updating_library
                    LibraryUpdateScope.Category -> MR.strings.updating_category
                    LibraryUpdateScope.Source -> MR.strings.updating_extension
                    LibraryUpdateScope.Type -> MR.strings.updating_type
                    LibraryUpdateScope.Group -> MR.strings.updating_group
                    LibraryUpdateScope.Selection -> MR.strings.updating_selection
                },
            )
        return listOfNotNull(
            if (total > 0) {
                context.stringResource(MR.strings.library_update_progress, scopeLabel, completed, total)
            } else {
                scopeLabel
            },
            skipped.takeIf { it > 0 }?.let {
                context.pluralStringResource(MR.plurals.library_update_skipped_by_rules, it, it)
            },
        )
            .joinToString(" · ")
    }
}

/** Which part of the library an update was asked to cover. */
enum class LibraryUpdateScope {
    Library,
    Category,
    Source,
    Type,

    /** More than one of category, source and entry type. */
    Group,

    /** Entries the user picked. */
    Selection,
    ;

    companion object {
        fun of(categoryId: Long?, sourceId: Long?, entryType: EntryType?): LibraryUpdateScope {
            val constraints = listOfNotNull(
                categoryId?.let { Category },
                sourceId?.let { Source },
                entryType?.let { Type },
            )
            return when (constraints.size) {
                0 -> Library
                1 -> constraints.single()
                else -> Group
            }
        }
    }
}
