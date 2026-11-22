package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.viewer.automaticBackgroundState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Reads the profiles of the pages right above and below those on screen, which the strip hasn't laid out, so the top
 * of the first page on screen follows the page above it and the bottom of the last the page below it, as they do once
 * those pages scroll into view. Profiles of pages that leave the strip are forgotten, and all of them when the settings
 * that open pages change, as the pages are read again then.
 */
@Composable
internal fun StripViewer.ReadNeighbourBackdrops() {
    val automatic = settings.bindings.automaticBackgroundState()
    val settingsKey = imageSettingsKey()
    DisposableEffect(settingsKey, automatic) { onDispose { backdrops.forget() } }
    LaunchedEffect(settingsKey, automatic) {
        if (!automatic) return@LaunchedEffect
        launch {
            snapshotFlow { items.mapNotNullTo(mutableSetOf()) { (it as? ReaderViewerItem.Page)?.page } }
                .collect(backdrops::keepOnly)
        }
        snapshotFlow { neighbourPages() }.distinctUntilChanged().collectLatest { pages ->
            coroutineScope { pages.filterNot(backdrops::has).forEach { launch { readBackdrop(it) } } }
        }
    }
}

/** The pages in the slots right before and after those on screen. */
private fun StripViewer.neighbourPages(): List<ReaderPage> {
    val visible = listState.layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return emptyList()
    return listOf(visible.first().index - 1, visible.last().index + 1)
        .mapNotNull { (items.getOrNull(it) as? ReaderViewerItem.Page)?.page }
}

/** Opens [page]'s image as the strip would show it and keeps its pieces' profiles. */
private suspend fun StripViewer.readBackdrop(page: ReaderPage) {
    try {
        val image = openOffScreenPage(page, settings.cropBorders) ?: return
        try {
            if (image is PageImage.Still) {
                backdrops.keep(page, image.pieceProfiles(stripPageLayout(image.size, settings.bindings)))
            }
        } finally {
            image.close()
        }
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        page.logcat(LogPriority.ERROR, e) { "The background beside page ${page.number} could not be read" }
    }
}
