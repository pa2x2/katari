package mihon.entry.interactions.book.epub

import androidx.fragment.app.FragmentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import org.json.JSONArray
import org.json.JSONTokener
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Layout

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

    fun goToProgression(
        navigator: EpubNavigatorFragment,
        progression: Double,
    ): Boolean {
        val locator = navigator.currentLocator.value
        return navigator.go(
            locator.copy(
                locations = locator.locations.copy(progression = progression.coerceIn(0.0, 1.0)),
            ),
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

    suspend fun resolveNavigationProgressions(
        navigator: EpubNavigatorFragment,
        navigation: List<ReadiumNavigationRow>,
        resourceId: String,
        paginated: Boolean,
    ): Map<String, Double> {
        val targets = navigation
            .map(ReadiumNavigationRow::item)
            .map(BookNavigationItem::target)
            .filter { it.resourceId == resourceId && it.fragments.isNotEmpty() }
            .distinctBy(BookLocator::navigationKey)
        if (targets.isEmpty()) return emptyMap()

        val fragments = targets.map { it.fragments.first() }
        val fragmentJson = JSONArray(fragments).toString()
        val result = navigator.evaluateJavascript(
            """
            (function() {
                var ids = $fragmentJson;
                var root = document.scrollingElement || document.documentElement;
                var horizontal = $paginated;
                return JSON.stringify(ids.map(function(rawId) {
                    var id = rawId;
                    try { id = decodeURIComponent(rawId); } catch (_) {}
                    var element = document.getElementById(id) || document.getElementsByName(id)[0];
                    if (!element) return null;
                    var rect = element.getBoundingClientRect();
                    var progression;
                    if (horizontal) {
                        var width = Math.max(root.scrollWidth, document.documentElement.scrollWidth, 1);
                        var x = rect.left + (window.scrollX || root.scrollLeft || 0);
                        var rtl = document.body && document.body.dir.toLowerCase() === "rtl";
                        progression = (rtl ? -x : x) / width;
                    } else {
                        var height = Math.max(root.scrollHeight, document.documentElement.scrollHeight, 1);
                        var y = rect.top + (window.scrollY || root.scrollTop || 0);
                        progression = y / height;
                    }
                    return Math.max(0, Math.min(1, progression));
                }));
            })();
            """.trimIndent(),
        ) ?: return emptyMap()
        val decoded = JSONTokener(result).nextValue()
        val values = when (decoded) {
            is JSONArray -> decoded
            is String -> runCatching { JSONArray(decoded) }.getOrNull()
            else -> null
        } ?: return emptyMap()

        return targets.mapIndexedNotNull { index, target ->
            values.optDouble(index).takeIf(Double::isFinite)?.let { target.navigationKey() to it }
        }.toMap()
    }

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
