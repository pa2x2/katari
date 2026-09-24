package mihon.entry.interactions.reader.settings

import android.os.Build
import androidx.compose.ui.graphics.BlendMode
import dev.icerock.moko.resources.StringResource
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingsProvider
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds
import tachiyomi.i18n.*

/**
 * Manga reader viewer settings contract.
 *
 * Every setting is declared once here as a [ViewerSettingDefinition]; the in-reader dialog, the app settings screen,
 * and reader runtime all resolve values through `ViewerSettingBinder` so per-entry overrides behave consistently.
 */
interface MangaReaderSettings : ViewerSettingsProvider {
    val reading: MangaReaderReadingDefinitions
    val display: MangaReaderDisplayDefinitions
    val eInk: MangaReaderEInkDefinitions
    val pager: MangaReaderPagerDefinitions
    val webtoon: MangaReaderWebtoonDefinitions
    val navigation: MangaReaderNavigationDefinitions
    val colorFilter: MangaReaderColorFilterDefinitions

    override val settings: List<ViewerSettingDefinition<*>>
        get() = reading.all +
            display.all +
            eInk.all +
            pager.all +
            webtoon.all +
            navigation.all +
            colorFilter.all

    override val sharedSettingDefinitions: Map<ReaderSharedSettingId, ViewerSettingDefinition<Boolean>>
        get() = mapOf(StandardReaderSharedSettingIds.NextChapterPreparation to reading.prepareNextChapter)

    enum class FlashColor {
        BLACK,
        WHITE,
        WHITE_BLACK,
    }

    enum class TappingInvertMode(
        val titleRes: StringResource,
        val shouldInvertHorizontal: Boolean = false,
        val shouldInvertVertical: Boolean = false,
    ) {
        NONE(MR.strings.tapping_inverted_none),
        HORIZONTAL(MR.strings.tapping_inverted_horizontal, shouldInvertHorizontal = true),
        VERTICAL(MR.strings.tapping_inverted_vertical, shouldInvertVertical = true),
        BOTH(MR.strings.tapping_inverted_both, shouldInvertHorizontal = true, shouldInvertVertical = true),
    }

    enum class ReaderHideThreshold(val threshold: Int) {
        HIGHEST(5),
        HIGH(13),
        LOW(31),
        LOWEST(47),
    }

    companion object {
        const val PROVIDER_ID = "builtin.manga.reader"
        const val READING_MODE_KEY = "reading_mode"
        const val ORIENTATION_KEY = "orientation"

        /** Reader capabilities the manga reader session provides to shared settings. */
        val READER_CAPABILITIES: Set<ReaderCapabilityId> = setOf(StandardReaderCapabilities.NextChapterPreparation)

        /** Keys used by app-level profile migrations that predate the settings provider. */
        const val CHAPTER_TRANSITION_PREFERENCE_KEY = "chapter_transition"
        const val VERTICAL_NAVIGATOR_PREFERENCE_KEY = "pref_vertical_navigator"
        const val VERTICAL_NAVIGATOR_ON_LEFT_PREFERENCE_KEY = "pref_vertical_navigator_on_left"

        const val AUTO_SCROLL_LEVEL_MIN = 0
        const val AUTO_SCROLL_LEVEL_MAX = 6
        const val AUTO_SCROLL_LEVEL_DEFAULT = 3

        const val WEBTOON_PADDING_MIN = 0
        const val WEBTOON_PADDING_MAX = 25

        const val MILLI_CONVERSION = 100

        val AUTO_SCROLL_SPEED_RANGE = AUTO_SCROLL_LEVEL_MIN..AUTO_SCROLL_LEVEL_MAX

        val AutoScrollLevelLabels = listOf(
            MR.strings.auto_scroll_speed_slowest,
            MR.strings.auto_scroll_speed_slower,
            MR.strings.auto_scroll_speed_slow,
            MR.strings.double_tap_anim_speed_normal,
            MR.strings.double_tap_anim_speed_fast,
            MR.strings.auto_scroll_speed_faster,
            MR.strings.auto_scroll_speed_fastest,
        )

        val TapZones = listOf(
            MR.strings.label_default,
            MR.strings.l_nav,
            MR.strings.kindlish_nav,
            MR.strings.edge_nav,
            MR.strings.right_and_left_nav,
            MR.strings.disabled_nav,
        )

        val ImageScaleType = listOf(
            MR.strings.scale_type_fit_screen,
            MR.strings.scale_type_stretch,
            MR.strings.scale_type_fit_width,
            MR.strings.scale_type_fit_height,
            MR.strings.scale_type_original_size,
            MR.strings.scale_type_smart_fit,
        )

        val ZoomStart = listOf(
            MR.strings.zoom_start_automatic,
            MR.strings.zoom_start_left,
            MR.strings.zoom_start_right,
            MR.strings.zoom_start_center,
        )

        val ColorFilterMode = buildList {
            addAll(
                listOf(
                    MR.strings.label_default to BlendMode.SrcOver,
                    MR.strings.filter_mode_multiply to BlendMode.Modulate,
                    MR.strings.filter_mode_screen to BlendMode.Screen,
                ),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addAll(
                    listOf(
                        MR.strings.filter_mode_overlay to BlendMode.Overlay,
                        MR.strings.filter_mode_lighten to BlendMode.Lighten,
                        MR.strings.filter_mode_darken to BlendMode.Darken,
                    ),
                )
            }
        }
    }
}
