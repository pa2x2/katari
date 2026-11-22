package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.strips.PageStrips
import app.katari.entry.interactions.manga.reader.image.strips.decodeStrips
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.viewer.key
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Decodes the pages the long strip is about to reach while the reader is still on the ones before, so they show at
 * once when they scroll in. The strip composes only the pages a short distance off screen, and a tall page, many
 * screens long, takes about half a second to decode on a mid-range phone, longer than a fling takes to cover that
 * distance; so the pages decoded here are counted in pages rather than distance: the [PAGES_AHEAD] slots past those on
 * screen in the direction the reader moves.
 *
 * A page composed later takes its strips from here ([take]), waiting for them if they are still being decoded, instead
 * of decoding them a second time; a page composed first is [claim]ed, so it isn't decoded here as well. Strips no page
 * took are freed once their page is neither on screen nor ahead. Used on the main thread only.
 */
internal class StripPagePreparation {
    private val prepared = mutableMapOf<ReaderPage, Deferred<PreparedStrips?>>()
    private val claimed = mutableSetOf<ReaderPage>()

    /** Starts decoding each of [ahead] in [scope] with [decode], and frees what was prepared for pages not [kept]. */
    fun update(
        scope: CoroutineScope,
        ahead: List<ReaderPage>,
        kept: Set<ReaderPage>,
        decode: suspend (ReaderPage) -> PreparedStrips?,
    ) {
        prepared.keys.filter { it !in kept }.forEach { prepared.remove(it)?.discard() }
        for (page in ahead) {
            if (page !in prepared && page !in claimed) prepared[page] = scope.async { decode(page) }
        }
    }

    /** Marks [page] as composed, which decodes its own strips unless it [take]s them from here. */
    fun claim(page: ReaderPage) {
        claimed += page
    }

    fun unclaim(page: ReaderPage) {
        claimed -= page
    }

    /**
     * The strips prepared for [page] if they were decoded as it shows, opened with [cropBorders] in [layout] and
     * [width] screen pixels wide, or null when there are none and the caller decodes them itself.
     */
    suspend fun take(page: ReaderPage, cropBorders: Boolean, layout: PageImageLayout, width: Int): PageStrips? {
        val preparing = prepared.remove(page) ?: return null
        val strips = try {
            preparing.await()
        } catch (e: CancellationException) {
            // Either the caller left, which frees what is being decoded, or the preparation stopped and the caller
            // decodes the page itself
            preparing.discard()
            currentCoroutineContext().ensureActive()
            return null
        } ?: return null
        if (strips.cropBorders == cropBorders && strips.layout == layout && strips.width == width) return strips.strips
        strips.strips.release()
        return null
    }

    /** Frees every strip prepared, as when the settings that open pages change or the strip leaves. */
    fun clear() {
        prepared.values.forEach { it.discard() }
        prepared.clear()
    }
}

/** [strips] decoded for a page opened with [cropBorders] in [layout], [width] screen pixels wide. */
internal class PreparedStrips(
    val cropBorders: Boolean,
    val layout: PageImageLayout,
    val width: Int,
    val strips: PageStrips,
)

// A decode cancelled partway frees what it decoded itself
private fun Deferred<PreparedStrips?>.discard() {
    if (isCompleted && !isCancelled) getCompleted()?.strips?.release() else cancel()
}

/**
 * Keeps [StripViewer.preparation] decoding the pages ahead of those on screen, for pages [width] screen pixels wide,
 * starting over when the settings that open pages change.
 */
@Composable
internal fun StripViewer.PrepareUpcomingPages(width: Int) {
    val settingsKey = imageSettingsKey()
    DisposableEffect(settingsKey, width) { onDispose { preparation.clear() } }
    LaunchedEffect(settingsKey, width) {
        if (width <= 0) return@LaunchedEffect
        val scope = this
        val direction = StripDirection()
        snapshotFlow { shownSlots() }.distinctUntilChanged().collect { shown ->
            if (shown == null) return@collect
            val backward = direction.follow(shown)
            val upcoming = shown.upcomingPages(backward)
            preparation.update(scope, upcoming.ahead, upcoming.kept) { prepareStrips(it, width) }
        }
    }
}

/** The slots [first] to [last] of [items] on screen. */
private data class ShownSlots(val items: List<ReaderViewerItem>, val first: Int, val last: Int)

// Null while the list hasn't laid out the slots it has now
private fun StripViewer.shownSlots(): ShownSlots? {
    val visible = listState.layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return null
    val slots = items
    if (visible.any { slots.getOrNull(it.index)?.key != it.key }) return null
    return ShownSlots(slots, visible.first().index, visible.last().index)
}

/**
 * Which way the reader moves through the strip, told by the slots on screen rather than by the last scroll: the strip
 * also scrolls itself, as when it keeps a page growing upward in place, which would read as the reader turning back.
 * Slots are followed by key, so slots put in front, as a previous chapter's pages, don't read as a move.
 */
private class StripDirection {
    private var firstKey: Any? = null
    private var lastKey: Any? = null
    private var backward = false

    /** Follows the slots now on screen and returns whether the reader moves toward the start. */
    fun follow(shown: ShownSlots): Boolean {
        val previousFirst = shown.items.indexOfFirst { it.key == firstKey }
        val previousLast = shown.items.indexOfFirst { it.key == lastKey }
        if (previousFirst != -1 && previousLast != -1) {
            if (shown.first < previousFirst || shown.last < previousLast) {
                backward = true
            } else if (shown.first > previousFirst || shown.last > previousLast) {
                backward = false
            }
        }
        firstKey = shown.items[shown.first].key
        lastKey = shown.items[shown.last].key
        return backward
    }
}

/** The pages to decode ahead, nearest first, and those whose prepared strips are kept: the ones on screen and ahead. */
private data class UpcomingPages(val ahead: List<ReaderPage>, val kept: Set<ReaderPage>)

private fun ShownSlots.upcomingPages(backward: Boolean): UpcomingPages {
    val ahead = if (backward) first - 1 downTo first - PAGES_AHEAD else last + 1..last + PAGES_AHEAD
    fun pageAt(index: Int) = (items.getOrNull(index) as? ReaderViewerItem.Page)?.page
    val aheadPages = ahead.mapNotNull(::pageAt)
    return UpcomingPages(aheadPages, (first..last).mapNotNullTo(aheadPages.toMutableSet(), ::pageAt))
}

/** Opens [page] as the strip opens the pages it composes, and decodes its strips [width] screen pixels wide. */
private suspend fun StripViewer.prepareStrips(page: ReaderPage, width: Int): PreparedStrips? {
    val request = pageRequest()
    return try {
        val image = openOffScreenPage(page, request.cropBorders) ?: return null
        try {
            if (image !is PageImage.Still) return null
            val layout = request.layout(page, image.size)
            image.decodeStrips(layout, width)?.let { PreparedStrips(request.cropBorders, layout, width, it) }
        } finally {
            image.close()
        }
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        page.logcat(LogPriority.ERROR, e) { "Page ${page.number} could not be prepared" }
        null
    }
}

// Two, so the page after a short one is ready too when it is a tall one
private const val PAGES_AHEAD = 2
