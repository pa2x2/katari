package app.katari.presentation.kit.layout

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.i18n.*

val CoverSize.labelRes: StringResource
    get() = when (this) {
        CoverSize.ExtraSmall -> MR.strings.pref_library_cover_size_extra_small
        CoverSize.Small -> MR.strings.pref_library_cover_size_small
        CoverSize.Medium -> MR.strings.pref_library_cover_size_medium
        CoverSize.Large -> MR.strings.pref_library_cover_size_large
        CoverSize.ExtraLarge -> MR.strings.pref_library_cover_size_extra_large
    }
