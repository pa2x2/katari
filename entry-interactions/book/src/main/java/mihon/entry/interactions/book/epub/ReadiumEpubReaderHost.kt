package mihon.entry.interactions.book.epub

import androidx.fragment.app.FragmentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Locator

/** Compile proof for a processor-owned reader surface; never crosses the generic BOOK boundary. */
internal class ReadiumEpubReaderHost(
    private val publicationSession: ReadiumPublicationSession,
) {
    fun createFragmentFactory(
        initialLocator: BookLocator?,
        initialPreferences: EpubPreferences,
        paginationListener: EpubNavigatorFragment.PaginationListener? = null,
    ): FragmentFactory {
        val publication = publicationSession.readiumPublication()
        return EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocator?.let { ReadiumLocatorAdapter.restore(it, publication) },
            initialPreferences = initialPreferences,
            paginationListener = paginationListener,
        )
    }

    fun observeLocations(
        navigator: EpubNavigatorFragment,
        scope: CoroutineScope,
        onLocation: suspend (BookLocator) -> Unit,
    ): Job = navigator.currentLocator
        .onEach { onLocation(ReadiumLocatorAdapter.adapt(it)) }
        .launchIn(scope)

    fun currentLocation(navigator: EpubNavigatorFragment): BookLocator? {
        return navigator.currentLocator.value?.let(ReadiumLocatorAdapter::adapt)
    }

    fun goForward(navigator: EpubNavigatorFragment): Boolean = navigator.goForward(animated = true)

    fun goBackward(navigator: EpubNavigatorFragment): Boolean = navigator.goBackward(animated = true)

    fun goToPage(
        navigator: EpubNavigatorFragment,
        pageIndex: Int,
        totalPages: Int,
    ): Boolean {
        if (totalPages <= 0 || pageIndex !in 0 until totalPages) return false
        val locator = navigator.currentLocator.value
        val progression = pageIndex.toDouble() / totalPages.toDouble()
        return navigator.go(
            locator.copy(locations = locator.locations.copy(progression = progression)),
            animated = false,
        )
    }

    fun goToNavigationItem(
        navigator: EpubNavigatorFragment,
        item: BookNavigationItem,
    ): Boolean {
        val publication = publicationSession.readiumPublication()
        val locator = ReadiumLocatorAdapter.restore(item.target, publication) ?: return false
        return navigator.go(locator, animated = false)
    }

    fun goToAdjacentSection(
        navigator: EpubNavigatorFragment,
        direction: Int,
    ): Boolean {
        val publication = publicationSession.readiumPublication()
        val currentHref = navigator.currentLocator.value.href.removeFragment()
        val index = publication.readingOrder.indexOfFirst { it.url().removeFragment() == currentHref }
        val target = publication.readingOrder.getOrNull(index + direction) ?: return false
        return navigator.go(target, animated = true)
    }

    fun goToSectionIndex(navigator: EpubNavigatorFragment, index: Int): Boolean {
        val target = publicationSession.readiumPublication().readingOrder.getOrNull(index) ?: return false
        return navigator.go(target, animated = false)
    }

    fun sectionIndex(locator: Locator): Int {
        val publication = publicationSession.readiumPublication()
        val href = locator.href.removeFragment()
        return publication.readingOrder.indexOfFirst { it.url().removeFragment() == href }
    }

    val sectionCount: Int
        get() = publicationSession.readiumPublication().readingOrder.size

    val isFixedLayout: Boolean
        get() = publicationSession.readiumPublication().metadata.layout == Layout.FIXED

    fun observeSettings(
        navigator: EpubNavigatorFragment,
        settings: ReadiumEpubSettingsBinding,
        scope: CoroutineScope,
    ): Job = settings.changes
        .onEach(navigator::submitPreferences)
        .launchIn(scope)
}
