package app.katari.entry.interactions.reader.settings

import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.custommaterialsymbols.CustomMaterialSymbols
import app.katari.icons.custommaterialsymbols.rounded.ReaderLeftToRight
import app.katari.icons.custommaterialsymbols.rounded.ReaderLongStrip
import app.katari.icons.custommaterialsymbols.rounded.ReaderLongStripGaps
import app.katari.icons.custommaterialsymbols.rounded.ReaderRightToLeft
import app.katari.icons.custommaterialsymbols.rounded.ReaderVertical
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.MobileGear
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

enum class ReadingMode(
    val stringRes: StringResource,
    val icon: ImageVector,
    val flagValue: Int,
    val direction: Direction? = null,
    val type: ViewerType? = null,
) {
    DEFAULT(MR.strings.label_default, MaterialSymbols.Rounded.MobileGear, 0x00000000),
    LEFT_TO_RIGHT(
        MR.strings.left_to_right_viewer,
        CustomMaterialSymbols.Rounded.ReaderLeftToRight,
        0x00000001,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    RIGHT_TO_LEFT(
        MR.strings.right_to_left_viewer,
        CustomMaterialSymbols.Rounded.ReaderRightToLeft,
        0x00000002,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    VERTICAL(
        MR.strings.vertical_viewer,
        CustomMaterialSymbols.Rounded.ReaderVertical,
        0x00000003,
        Direction.Vertical,
        ViewerType.Pager,
    ),
    WEBTOON(
        MR.strings.webtoon_viewer,
        CustomMaterialSymbols.Rounded.ReaderLongStrip,
        0x00000004,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    CONTINUOUS_VERTICAL(
        MR.strings.vertical_plus_viewer,
        CustomMaterialSymbols.Rounded.ReaderLongStripGaps,
        0x00000005,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    ;

    companion object {
        const val MASK = 0x00000007

        fun fromPreference(preference: Int?): ReadingMode = entries.find { it.flagValue == preference } ?: DEFAULT

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode.type is ViewerType.Pager
        }
    }

    sealed interface Direction {
        data object Horizontal : Direction
        data object Vertical : Direction
    }

    sealed interface ViewerType {
        data object Pager : ViewerType
        data object Webtoon : ViewerType
    }
}
