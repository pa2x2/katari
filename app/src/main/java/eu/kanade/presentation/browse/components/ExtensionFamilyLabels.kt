package eu.kanade.presentation.browse.components

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import tachiyomi.i18n.*

internal val ExtensionFamily.titleRes: StringResource
    get() = when (this) {
        ExtensionFamily.KATARI -> MR.strings.ext_family_katari
        ExtensionFamily.TACHIYOMI -> MR.strings.ext_family_tachiyomi
        ExtensionFamily.ANIYOMI -> MR.strings.ext_family_aniyomi
    }

internal val ExtensionFamily.versionsText: String
    get() = libVersions.joinToString()
