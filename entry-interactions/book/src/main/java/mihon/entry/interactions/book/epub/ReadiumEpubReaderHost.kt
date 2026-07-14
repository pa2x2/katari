package mihon.entry.interactions.book.epub

import androidx.fragment.app.FragmentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.book.api.BookLocator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences

/** Compile proof for a processor-owned reader surface; never crosses the generic BOOK boundary. */
internal class ReadiumEpubReaderHost(
    private val publicationSession: ReadiumPublicationSession,
) {
    fun createFragmentFactory(
        initialLocator: BookLocator?,
        initialPreferences: EpubPreferences,
    ): FragmentFactory {
        val publication = publicationSession.readiumPublication()
        return EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocator?.let { ReadiumLocatorAdapter.restore(it, publication) },
            initialPreferences = initialPreferences,
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

    fun observeSettings(
        navigator: EpubNavigatorFragment,
        settings: ReadiumEpubSettingsBinding,
        scope: CoroutineScope,
    ): Job = settings.changes
        .onEach(navigator::submitPreferences)
        .launchIn(scope)
}
