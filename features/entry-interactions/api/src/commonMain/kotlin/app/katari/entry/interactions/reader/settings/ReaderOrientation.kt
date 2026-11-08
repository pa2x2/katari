package app.katari.entry.interactions.reader.settings

import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Mobile
import app.katari.icons.materialsymbols.rounded.MobileLandscape
import app.katari.icons.materialsymbols.rounded.MobileLockLandscape
import app.katari.icons.materialsymbols.rounded.MobileLockPortrait
import app.katari.icons.materialsymbols.rounded.MobileRotate
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

enum class ReaderOrientation(
    val stringRes: StringResource,
    val icon: ImageVector,
    val flagValue: Int,
) {
    DEFAULT(
        MR.strings.label_default,
        MaterialSymbols.Rounded.MobileRotate,
        0x00000000,
    ),
    FREE(
        MR.strings.rotation_free,
        MaterialSymbols.Rounded.MobileRotate,
        0x00000008,
    ),
    PORTRAIT(
        MR.strings.rotation_portrait,
        MaterialSymbols.Rounded.Mobile,
        0x00000010,
    ),
    LANDSCAPE(
        MR.strings.rotation_landscape,
        MaterialSymbols.Rounded.MobileLandscape,
        0x00000018,
    ),
    LOCKED_PORTRAIT(
        MR.strings.rotation_force_portrait,
        MaterialSymbols.Rounded.MobileLockPortrait,
        0x00000020,
    ),
    LOCKED_LANDSCAPE(
        MR.strings.rotation_force_landscape,
        MaterialSymbols.Rounded.MobileLockLandscape,
        0x00000028,
    ),
    REVERSE_PORTRAIT(
        MR.strings.rotation_reverse_portrait,
        MaterialSymbols.Rounded.Mobile,
        0x00000030,
    ),
    ;

    companion object {
        const val MASK = 0x00000038

        fun fromPreference(preference: Int?): ReaderOrientation = entries.find { it.flagValue == preference } ?: DEFAULT
    }
}
