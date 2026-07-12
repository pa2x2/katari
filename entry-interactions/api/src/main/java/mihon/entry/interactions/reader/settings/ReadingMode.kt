package mihon.entry.interactions.reader.settings

import androidx.annotation.DrawableRes
import dev.icerock.moko.resources.StringResource
import mihon.entry.interactions.api.R
import tachiyomi.i18n.MR

enum class ReadingMode(
    val stringRes: StringResource,
    @DrawableRes val iconRes: Int,
    val flagValue: Int,
    val direction: Direction? = null,
    val type: ViewerType? = null,
) {
    DEFAULT(MR.strings.label_default, R.drawable.ic_reader_default_24dp, 0x00000000),
    LEFT_TO_RIGHT(
        MR.strings.left_to_right_viewer,
        R.drawable.ic_reader_ltr_24dp,
        0x00000001,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    RIGHT_TO_LEFT(
        MR.strings.right_to_left_viewer,
        R.drawable.ic_reader_rtl_24dp,
        0x00000002,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    VERTICAL(
        MR.strings.vertical_viewer,
        R.drawable.ic_reader_vertical_24dp,
        0x00000003,
        Direction.Vertical,
        ViewerType.Pager,
    ),
    WEBTOON(
        MR.strings.webtoon_viewer,
        R.drawable.ic_reader_webtoon_24dp,
        0x00000004,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    CONTINUOUS_VERTICAL(
        MR.strings.vertical_plus_viewer,
        R.drawable.ic_reader_continuous_vertical_24dp,
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
