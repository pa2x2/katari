package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId

/**
 * Entry-scoped bindings for every manga reader setting.
 *
 * Created once per opened entry from one persisted snapshot so the in-reader UI and the live reader resolve the same
 * processor default, profile, and entry override values.
 */
internal class MangaReaderSettingsBindings private constructor(
    private val definitions: MangaReaderSettings,
    private val bindings: Map<ViewerSettingId, ViewerSettingBinding<*>>,
    val sharedSettings: Map<ReaderSharedSettingId, ViewerSettingBinding<Boolean>>,
) {
    val readingMode: ViewerSettingBinding<Int> get() = binding(definitions.reading.readingMode)
    val orientation: ViewerSettingBinding<Int> get() = binding(definitions.reading.orientation)
    val chapterTransition: ViewerSettingBinding<ChapterTransitionMode>
        get() = binding(definitions.reading.chapterTransition)
    val prepareNextChapter: ViewerSettingBinding<Boolean> get() = binding(definitions.reading.prepareNextChapter)
    val pageTransitions: ViewerSettingBinding<Boolean> get() = binding(definitions.reading.pageTransitions)
    val readWithLongTap: ViewerSettingBinding<Boolean> get() = binding(definitions.reading.readWithLongTap)
    val skipRead: ViewerSettingBinding<Boolean> get() = binding(definitions.reading.skipRead)
    val skipFiltered: ViewerSettingBinding<Boolean> get() = binding(definitions.reading.skipFiltered)
    val skipDuplicate: ViewerSettingBinding<Boolean> get() = binding(definitions.reading.skipDuplicate)
    val folderPerManga: ViewerSettingBinding<Boolean> get() = binding(definitions.reading.folderPerManga)

    val readerTheme: ViewerSettingBinding<Int> get() = binding(definitions.display.readerTheme)
    val showPageNumber: ViewerSettingBinding<Boolean> get() = binding(definitions.display.showPageNumber)
    val fullscreen: ViewerSettingBinding<Boolean> get() = binding(definitions.display.fullscreen)
    val drawUnderCutout: ViewerSettingBinding<Boolean> get() = binding(definitions.display.drawUnderCutout)
    val keepScreenOn: ViewerSettingBinding<Boolean> get() = binding(definitions.display.keepScreenOn)
    val showReadingMode: ViewerSettingBinding<Boolean> get() = binding(definitions.display.showReadingMode)
    val showNavigationOverlayOnStart: ViewerSettingBinding<Boolean>
        get() = binding(definitions.display.showNavigationOverlayOnStart)
    val showNavigationOverlayNewUser: ViewerSettingBinding<Boolean>
        get() = binding(definitions.display.showNavigationOverlayNewUser)
    val doubleTapAnimSpeed: ViewerSettingBinding<Int> get() = binding(definitions.display.doubleTapAnimSpeed)

    val flashOnPageChange: ViewerSettingBinding<Boolean> get() = binding(definitions.eInk.flashOnPageChange)
    val flashDurationMillis: ViewerSettingBinding<Int> get() = binding(definitions.eInk.flashDurationMillis)
    val flashPageInterval: ViewerSettingBinding<Int> get() = binding(definitions.eInk.flashPageInterval)
    val flashColor: ViewerSettingBinding<MangaReaderSettings.FlashColor> get() = binding(definitions.eInk.flashColor)

    val pagerNavigationMode: ViewerSettingBinding<Int> get() = binding(definitions.pager.navigationMode)
    val pagerNavigationInverted: ViewerSettingBinding<MangaReaderSettings.TappingInvertMode>
        get() = binding(definitions.pager.navigationInverted)
    val imageScaleType: ViewerSettingBinding<Int> get() = binding(definitions.pager.imageScaleType)
    val zoomStart: ViewerSettingBinding<Int> get() = binding(definitions.pager.zoomStart)
    val cropBorders: ViewerSettingBinding<Boolean> get() = binding(definitions.pager.cropBorders)
    val landscapeZoom: ViewerSettingBinding<Boolean> get() = binding(definitions.pager.landscapeZoom)
    val navigateToPan: ViewerSettingBinding<Boolean> get() = binding(definitions.pager.navigateToPan)
    val dualPageSplitPaged: ViewerSettingBinding<Boolean> get() = binding(definitions.pager.dualPageSplit)
    val dualPageInvertPaged: ViewerSettingBinding<Boolean> get() = binding(definitions.pager.dualPageInvert)
    val dualPageRotateToFit: ViewerSettingBinding<Boolean> get() = binding(definitions.pager.dualPageRotateToFit)
    val dualPageRotateToFitInvert: ViewerSettingBinding<Boolean>
        get() = binding(definitions.pager.dualPageRotateToFitInvert)

    val webtoonNavigationMode: ViewerSettingBinding<Int> get() = binding(definitions.webtoon.navigationMode)
    val webtoonNavigationInverted: ViewerSettingBinding<MangaReaderSettings.TappingInvertMode>
        get() = binding(definitions.webtoon.navigationInverted)
    val webtoonSidePadding: ViewerSettingBinding<Int> get() = binding(definitions.webtoon.sidePadding)
    val readerHideThreshold: ViewerSettingBinding<MangaReaderSettings.ReaderHideThreshold>
        get() = binding(definitions.webtoon.hideThreshold)
    val cropBordersWebtoon: ViewerSettingBinding<Boolean> get() = binding(definitions.webtoon.cropBorders)
    val dualPageSplitWebtoon: ViewerSettingBinding<Boolean> get() = binding(definitions.webtoon.dualPageSplit)
    val dualPageInvertWebtoon: ViewerSettingBinding<Boolean> get() = binding(definitions.webtoon.dualPageInvert)
    val dualPageRotateToFitWebtoon: ViewerSettingBinding<Boolean>
        get() = binding(definitions.webtoon.dualPageRotateToFit)
    val dualPageRotateToFitInvertWebtoon: ViewerSettingBinding<Boolean>
        get() = binding(definitions.webtoon.dualPageRotateToFitInvert)
    val webtoonDoubleTapZoom: ViewerSettingBinding<Boolean> get() = binding(definitions.webtoon.doubleTapZoom)
    val webtoonDisableZoomOut: ViewerSettingBinding<Boolean> get() = binding(definitions.webtoon.disableZoomOut)

    val verticalNavigator: ViewerSettingBinding<Set<ReadingMode>>
        get() = binding(definitions.navigation.verticalNavigator)
    val verticalNavigatorOnLeft: ViewerSettingBinding<Boolean>
        get() = binding(definitions.navigation.verticalNavigatorOnLeft)
    val verticalNavigatorHeight: ViewerSettingBinding<Int>
        get() = binding(definitions.navigation.verticalNavigatorHeight)
    val volumeKeys: ViewerSettingBinding<Boolean> get() = binding(definitions.navigation.volumeKeys)
    val volumeKeysInverted: ViewerSettingBinding<Boolean> get() = binding(definitions.navigation.volumeKeysInverted)
    val autoScrollEnabled: ViewerSettingBinding<Boolean> get() = binding(definitions.navigation.autoScrollEnabled)
    val autoScrollSpeed: ViewerSettingBinding<Int> get() = binding(definitions.navigation.autoScrollSpeed)

    val customBrightness: ViewerSettingBinding<Boolean> get() = binding(definitions.colorFilter.customBrightness)
    val customBrightnessValue: ViewerSettingBinding<Int>
        get() = binding(definitions.colorFilter.customBrightnessValue)
    val colorFilter: ViewerSettingBinding<Boolean> get() = binding(definitions.colorFilter.colorFilter)
    val colorFilterValue: ViewerSettingBinding<Int> get() = binding(definitions.colorFilter.colorFilterValue)
    val colorFilterMode: ViewerSettingBinding<Int> get() = binding(definitions.colorFilter.colorFilterMode)
    val grayscale: ViewerSettingBinding<Boolean> get() = binding(definitions.colorFilter.grayscale)
    val invertedColors: ViewerSettingBinding<Boolean> get() = binding(definitions.colorFilter.invertedColors)

    /** Clears this entry's overrides without touching profile values, matching the book reader reset semantics. */
    suspend fun clearEntryOverrides() {
        bindings.values
            .filter { it.definition.scope == ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE }
            .forEach { it.clearEntryOverride() }
    }

    private fun <T> binding(definition: ViewerSettingDefinition<T>): ViewerSettingBinding<T> {
        @Suppress("UNCHECKED_CAST")
        return bindings.getValue(definition.id) as ViewerSettingBinding<T>
    }

    companion object {
        suspend fun create(
            definitions: MangaReaderSettings,
            binder: ViewerSettingBinder,
            entryId: Long,
        ): MangaReaderSettingsBindings {
            val entryBinder = binder.initializeEntry(entryId)
            val bindings = definitions.settings.associate { definition ->
                definition.id to entryBinder.bind(definition)
            }
            val sharedSettings = definitions.sharedSettingDefinitions.mapValues { (_, definition) ->
                @Suppress("UNCHECKED_CAST")
                bindings.getValue(definition.id) as ViewerSettingBinding<Boolean>
            }
            return MangaReaderSettingsBindings(
                definitions = definitions,
                bindings = bindings,
                sharedSettings = sharedSettings,
            )
        }
    }
}
