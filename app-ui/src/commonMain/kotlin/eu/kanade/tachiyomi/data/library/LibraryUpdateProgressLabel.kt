package eu.kanade.tachiyomi.data.library

import mihon.core.platform.PlatformContext
import tachiyomi.domain.library.update.model.LibraryUpdateProgress
import tachiyomi.domain.library.update.model.LibraryUpdateScope
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.pluralStringResource
import tachiyomi.i18n.platform.stringResource

/** "Updating Reading · 12 / 37 · 3 skipped by rules", worded the same in the library and the shade. */
fun LibraryUpdateProgress.label(context: PlatformContext): String {
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
