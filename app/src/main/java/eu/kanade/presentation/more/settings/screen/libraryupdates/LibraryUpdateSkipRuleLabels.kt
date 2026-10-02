package eu.kanade.presentation.more.settings.screen.libraryupdates

import mihon.feature.library.update.planning.LibraryUpdateSkipRule
import tachiyomi.i18n.*

internal val LibraryUpdateSkipRule.titleRes
    get() = when (this) {
        LibraryUpdateSkipRule.COMPLETED -> MR.strings.library_updates_skip_completed
        LibraryUpdateSkipRule.UNSEEN -> MR.strings.library_updates_skip_unseen
        LibraryUpdateSkipRule.NOT_STARTED -> MR.strings.library_updates_skip_not_started
        LibraryUpdateSkipRule.OUTSIDE_RELEASE_PERIOD -> MR.strings.library_updates_skip_outside_release
    }
