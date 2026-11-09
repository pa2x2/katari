package eu.kanade.presentation.library.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.i18n.*

val LibraryPinnedDisplayStyle.labelRes: StringResource
    get() = when (this) {
        LibraryPinnedDisplayStyle.TonalGroup -> MR.strings.pref_library_pinned_style_tonal
        LibraryPinnedDisplayStyle.Shelf -> MR.strings.pref_library_pinned_style_shelf
    }
