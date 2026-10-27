package eu.kanade.presentation.library.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.i18n.*

internal val CoverSize.labelRes: StringResource
    get() = when (this) {
        CoverSize.Auto -> MR.strings.label_auto
        CoverSize.Small -> MR.strings.pref_library_cover_size_small
        CoverSize.Medium -> MR.strings.pref_library_cover_size_medium
        CoverSize.Large -> MR.strings.pref_library_cover_size_large
        CoverSize.ExtraLarge -> MR.strings.pref_library_cover_size_extra_large
    }
