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
    private var pendingNavigationIndex: Int? = null
    internal var effectiveReadingDirection = BookReadingDirection.LEFT_TO_RIGHT
    private val seekDispatcher = ThrottledLatestDispatcher<Int>(
        scope = activity.lifecycleScope,
        intervalMillis = SEEK_PREVIEW_INTERVAL_MILLIS,
        dispatch = ::goToPageInSection,
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
                val tapNavigation = readerSettings.tapNavigation.resolveProfile().effectiveValue
                val width = fragment.publicationView.width.takeIf { it > 0 } ?: return false
                val x = event.point.x / width.toFloat()
                if (!tapNavigation || x in TAP_PREVIOUS_END..TAP_NEXT_START) {
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

    internal fun previewPageInSection(pageIndex: Int) {
        uiState = uiState.copy(currentPage = pageIndex + 1)
        seekDispatcher.preview(pageIndex)
    }

    internal fun finishPageInSection(pageIndex: Int) {
        seekDispatcher.finish(pageIndex)
    }

    internal fun resolveCurrentNavigation() {
        val fragment = navigator ?: return
        val host = readerHost ?: return
        val locator = uiState.currentLocator ?: return
        val key = "${locator.resourceId}|$resourceTotalPages"
        if (navigationResolutionKey == key) return
        navigationResolutionKey = key
        navigationResolutionJob?.cancel()
        navigationResolutionJob = lifecycleScope.launch {
            val positions = host.resolveNavigationProgressions(
                navigator = fragment,
                navigation = navigation,
                resourceId = locator.resourceId,
                totalPages = resourceTotalPages,
                readingDirection = effectiveReadingDirection,
            )
            if (uiState.currentLocator?.resourceId != locator.resourceId) return@launch
            resolvedNavigationPositions.putAll(positions)
            recalculateSectionMetrics()
        }
    }

    internal fun recalculateSectionMetrics() {
        val locator = uiState.currentLocator ?: return
        val preferredIndex = pendingNavigationIndex ?: uiState.currentSectionIndex
        val paginatedMetrics = resolvePaginatedSectionMetrics(
            navigation = navigation,
            locator = locator,
            resolvedPositions = resolvedNavigationPositions,
            currentPageIndex = resourceCurrentPage - 1,
            totalPages = resourceTotalPages,
            preferredIndex = preferredIndex,
        )
        val fallbackIndex = preferredIndex
            .takeIf { it in navigation.indices && navigation[it].item.target.resourceId == locator.resourceId }
            ?: navigation.indexOfFirst { it.item.target.resourceId == locator.resourceId }
        val sectionIndex = paginatedMetrics?.index ?: fallbackIndex
        val startPageIndex = paginatedMetrics?.startPageIndex ?: 0
        val endPageIndex = paginatedMetrics?.endPageIndex ?: resourceTotalPages
        sectionStartPageIndex = startPageIndex
        val totalPages = (endPageIndex - startPageIndex).coerceAtLeast(1)
        val currentPage = (resourceCurrentPage - startPageIndex).coerceIn(1, totalPages)

        if (pendingNavigationIndex == sectionIndex) pendingNavigationIndex = null
        uiState = uiState.copy(
            sectionTitle = navigation.getOrNull(sectionIndex)?.item?.title,
            currentSectionIndex = sectionIndex,
            currentPage = currentPage,
            totalPages = totalPages,
        )
    }
}
