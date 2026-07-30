@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package mihon.entry.interactions.book.epub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookReadingDirection
import mihon.entry.interactions.reader.settings.BookReaderLayoutMode
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Locator
import uy.kohesive.injekt.api.get

/** Processor-owned EPUB reader surface. Generic BOOK code only launches this entry point. */
internal class ReadiumEpubNavigationController(
    private val activity: ReadiumEpubReaderActivity,
) : AutoCloseable {
    private val resolvedNavigationPositions = mutableMapOf<String, ReadiumNavigationPosition>()
    private var navigationResolutionJob: Job? = null
    private var navigationResolutionKey: String? = null
    private var resourceCurrentPage = 1
    private var resourceTotalPages = 1
    private var sectionStartPageIndex = 0
    private var sectionStartProgression = 0.0
    private var sectionEndProgression = 1.0
    private var pendingNavigationIndex: Int? = null
    internal var effectiveReadingDirection = BookReadingDirection.LEFT_TO_RIGHT
    private val seekDispatcher = ThrottledLatestDispatcher<ReaderSeekTarget>(
        scope = activity.lifecycleScope,
        intervalMillis = SEEK_PREVIEW_INTERVAL_MILLIS,
        dispatch = ::applySeekTarget,
    )

    private val retainedSession get() = activity.retainedSession
    private val lifecycleScope get() = activity.lifecycleScope
    private val readerHost get() = activity.readerHost
    private val navigator get() = activity.navigator
    private val settings get() = activity.settings
    private val navigation get() = activity.navigation
    private val translationController get() = activity.translationController
    private var uiState
        get() = activity.uiState
        set(value) {
            activity.uiState = value
        }

    internal fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
        resourceTotalPages = totalPages.coerceAtLeast(1)
        resourceCurrentPage = physicalPageIndexToLogical(
            pageIndex = pageIndex,
            totalPages = resourceTotalPages,
            readingDirection = effectiveReadingDirection,
        ) + 1
        updateLocation(locator)
    }

    internal fun onPageLoaded() {
        navigationResolutionKey = null
        resolveCurrentNavigation()
    }

    internal fun updateBookLocation(locator: BookLocator) {
        retainedSession.updateLocation(locator)
        uiState = uiState.copy(currentLocator = locator)
        recalculateSectionMetrics()
        resolveCurrentNavigation()
    }

    private fun setMenuVisibility(visible: Boolean) = activity.setMenuVisibility(visible)

    override fun close() {
        seekDispatcher.cancel()
        navigationResolutionJob?.cancel()
        navigationResolutionJob = null
    }

    private sealed interface ReaderSeekTarget {
        data class Page(val index: Int) : ReaderSeekTarget
        data class Progress(val value: Float) : ReaderSeekTarget
    }

    private companion object {
        const val TAP_PREVIOUS_END = 0.33f
        const val TAP_NEXT_START = 0.67f
        const val SEEK_PREVIEW_INTERVAL_MILLIS = 75L
    }

    internal fun updateLocation(locator: Locator) {
        val adapted = ReadiumLocatorAdapter.adapt(locator)
        retainedSession.updateLocation(adapted)
        uiState = uiState.copy(
            currentLocator = adapted,
        )
        recalculateSectionMetrics()
        resolveCurrentNavigation()
    }

    internal fun createInputListener(readingDirection: BookReadingDirection?): InputListener {
        return object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                if (translationController?.dismissTranslationOnReaderTap() == true) {
                    return true
                }
                val fragment = navigator ?: return false
                val readerSettings = settings ?: return false
                val paginated = readerSettings.layoutMode.state.value.effectiveValue ==
                    BookReaderLayoutMode.PAGINATED.serializedValue
                val tapNavigation = readerSettings.tapNavigation.resolveProfile().effectiveValue
                val width = fragment.publicationView.width.takeIf { it > 0 } ?: return false
                val x = event.point.x / width.toFloat()
                if (!paginated || !tapNavigation || x in TAP_PREVIOUS_END..TAP_NEXT_START) {
                    setMenuVisibility(!uiState.menuVisible)
                    return true
                }

                val forward = when (readingDirection) {
                    BookReadingDirection.RIGHT_TO_LEFT -> x < TAP_PREVIOUS_END
                    else -> x > TAP_NEXT_START
                }
                if (forward) {
                    readerHost?.goForward(fragment)
                } else {
                    readerHost?.goBackward(fragment)
                }
                setMenuVisibility(false)
                return true
            }
        }
    }

    internal fun goToAdjacentSection(direction: Int) {
        val currentIndex = uiState.currentSectionIndex
        if (currentIndex !in navigation.indices) return
        val currentTarget = navigation[currentIndex].item.target.navigationKey()
        val target = generateSequence(currentIndex + direction) { it + direction }
            .takeWhile { it in navigation.indices }
            .firstOrNull { navigation[it].item.target.navigationKey() != currentTarget }
            ?: return
        goToNavigationItem(navigation[target].item)
    }

    internal fun goToNavigationItem(item: BookNavigationItem) {
        translationController?.clearSelection()
        val fragment = navigator ?: return
        val index = navigation.indexOfFirst { it.item == item }
        if (index >= 0) {
            pendingNavigationIndex = index
            uiState = uiState.copy(
                currentSectionIndex = index,
                sectionTitle = item.title,
                sectionProgress = 0f,
                currentPage = 1,
            )
        }
        if (readerHost?.goToNavigationItem(fragment, item) != true) {
            pendingNavigationIndex = null
            recalculateSectionMetrics()
        }
    }

    internal fun goToPageInSection(pageIndex: Int) {
        translationController?.clearSelection()
        val fragment = navigator ?: return
        val target = (sectionStartPageIndex + pageIndex).coerceIn(0, resourceTotalPages - 1)
        uiState = uiState.copy(currentPage = pageIndex + 1)
        readerHost?.goToPage(fragment, target, resourceTotalPages)
    }

    internal fun goToProgressInSection(progress: Float) {
        translationController?.clearSelection()
        val fragment = navigator ?: return
        val safeProgress = progress.coerceIn(0f, 1f)
        val target = sectionStartProgression +
            (sectionEndProgression - sectionStartProgression) * safeProgress
        uiState = uiState.copy(sectionProgress = safeProgress)
        readerHost?.goToProgression(fragment, target)
    }

    internal fun previewPageInSection(pageIndex: Int) {
        uiState = uiState.copy(currentPage = pageIndex + 1)
        seekDispatcher.preview(ReaderSeekTarget.Page(pageIndex))
    }

    internal fun finishPageInSection(pageIndex: Int) {
        seekDispatcher.finish(ReaderSeekTarget.Page(pageIndex))
    }

    internal fun previewProgressInSection(progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        uiState = uiState.copy(sectionProgress = safeProgress)
        seekDispatcher.preview(ReaderSeekTarget.Progress(safeProgress))
    }

    internal fun finishProgressInSection(progress: Float) {
        seekDispatcher.finish(ReaderSeekTarget.Progress(progress.coerceIn(0f, 1f)))
    }

    private fun applySeekTarget(target: ReaderSeekTarget) {
        when (target) {
            is ReaderSeekTarget.Page -> goToPageInSection(target.index)
            is ReaderSeekTarget.Progress -> goToProgressInSection(target.value)
        }
    }

    internal fun resolveCurrentNavigation() {
        val fragment = navigator ?: return
        val host = readerHost ?: return
        val locator = uiState.currentLocator ?: return
        val paginated = isPaginated()
        val key = "${locator.resourceId}|$paginated|$resourceTotalPages"
        if (navigationResolutionKey == key) return
        navigationResolutionKey = key
        navigationResolutionJob?.cancel()
        navigationResolutionJob = lifecycleScope.launch {
            val positions = host.resolveNavigationProgressions(
                navigator = fragment,
                navigation = navigation,
                resourceId = locator.resourceId,
                paginated = paginated,
                totalPages = resourceTotalPages,
                readingDirection = effectiveReadingDirection,
            )
            if (uiState.currentLocator?.resourceId != locator.resourceId || isPaginated() != paginated) return@launch
            resolvedNavigationPositions.putAll(positions)
            recalculateSectionMetrics()
        }
    }

    internal fun recalculateSectionMetrics() {
        val locator = uiState.currentLocator ?: return
        val preferredIndex = pendingNavigationIndex ?: uiState.currentSectionIndex
        val paginatedMetrics = if (isPaginated()) {
            resolvePaginatedSectionMetrics(
                navigation = navigation,
                locator = locator,
                resolvedPositions = resolvedNavigationPositions,
                currentPageIndex = resourceCurrentPage - 1,
                totalPages = resourceTotalPages,
                preferredIndex = preferredIndex,
            )
        } else {
            null
        }
        val scrollingMetrics = if (paginatedMetrics == null) {
            resolveSectionMetrics(
                navigation = navigation,
                locator = locator,
                resolvedPositions = resolvedNavigationPositions,
                preferredIndex = preferredIndex,
            )
        } else {
            null
        }
        val fallbackIndex = preferredIndex
            .takeIf { it in navigation.indices && navigation[it].item.target.resourceId == locator.resourceId }
            ?: navigation.indexOfFirst { it.item.target.resourceId == locator.resourceId }
        val sectionIndex = paginatedMetrics?.index ?: scrollingMetrics?.index ?: fallbackIndex
        val sectionStart = paginatedMetrics?.startProgression ?: scrollingMetrics?.startProgression ?: 0.0
        val sectionEnd = paginatedMetrics?.endProgression ?: scrollingMetrics?.endProgression ?: 1.0
        sectionStartProgression = sectionStart
        sectionEndProgression = sectionEnd

        val currentProgression = locator.progression ?: sectionStart
        val sectionProgress = if (sectionEnd - sectionStart > 0.0001) {
            ((currentProgression - sectionStart) / (sectionEnd - sectionStart)).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val startPageIndex = paginatedMetrics?.startPageIndex
            ?: (sectionStart * resourceTotalPages).toInt().coerceIn(0, resourceTotalPages - 1)
        val endPageIndex = paginatedMetrics?.endPageIndex
            ?: (sectionEnd * resourceTotalPages).toInt().coerceIn(startPageIndex + 1, resourceTotalPages)
        sectionStartPageIndex = startPageIndex
        val totalPages = (endPageIndex - startPageIndex).coerceAtLeast(1)
        val currentPage = (resourceCurrentPage - startPageIndex).coerceIn(1, totalPages)

        if (pendingNavigationIndex == sectionIndex) pendingNavigationIndex = null
        uiState = uiState.copy(
            sectionTitle = navigation.getOrNull(sectionIndex)?.item?.title,
            currentSectionIndex = sectionIndex,
            currentPage = currentPage,
            totalPages = totalPages,
            sectionProgress = sectionProgress.toFloat(),
        )
    }

    internal fun isPaginated(): Boolean = uiState.fixedLayout ||
        settings?.layoutMode?.state?.value?.effectiveValue == BookReaderLayoutMode.PAGINATED.serializedValue
}
