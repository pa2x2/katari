package mihon.entry.interactions.book.epub

import androidx.fragment.app.FragmentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.book.api.BookLocator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment

/** Compile proof for a processor-owned reader surface; never crosses the generic BOOK boundary. */
internal class ReadiumEpubReaderHost(
    private val publicationSession: ReadiumPublicationSession,
) {
    fun createFragmentFactory(initialLocator: BookLocator?): FragmentFactory {
        val publication = publicationSession.readiumPublication()
        return EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocator?.let { ReadiumLocatorAdapter.restore(it, publication) },
        )
    }

    fun observeLocations(
        navigator: EpubNavigatorFragment,
        scope: CoroutineScope,
        onLocation: (BookLocator) -> Unit,
    ): Job = navigator.currentLocator
        .onEach { onLocation(ReadiumLocatorAdapter.adapt(it)) }
        .launchIn(scope)
}
