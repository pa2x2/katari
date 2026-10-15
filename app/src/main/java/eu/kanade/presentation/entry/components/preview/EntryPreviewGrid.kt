package eu.kanade.presentation.entry.components.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import eu.kanade.tachiyomi.ui.entry.EntryViewModel
import kotlinx.coroutines.flow.flowOf
import mihon.core.common.image.progressive.ProgressiveImageState
import mihon.entry.interactions.media.EntryPreviewPage
import mihon.entry.interactions.media.EntryPreviewPageStatus
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.BrokenImage
import mihon.presentation.kit.content.EntryMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.CircularProgressIndicator
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

enum class PreviewSizeUi {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
}

private val PreviewGridSpacing = KatariTheme.spacing.m

internal fun previewGridColumnCount(
    size: PreviewSizeUi,
    availableWidth: Dp,
): Int {
    val preferredTileWidth = when (size) {
        PreviewSizeUi.SMALL -> EntryMetrics.previewTileSmall
        PreviewSizeUi.MEDIUM -> EntryMetrics.previewTileMedium
        PreviewSizeUi.LARGE -> EntryMetrics.previewTileLarge
        PreviewSizeUi.EXTRA_LARGE -> EntryMetrics.previewTileExtraLarge
    }

    return ((availableWidth + PreviewGridSpacing) / (preferredTileWidth + PreviewGridSpacing))
        .toInt()
        .coerceAtLeast(1)
}

@Composable
internal fun PreviewGrid(
    pages: List<EntryViewModel.PreviewPage>,
    size: PreviewSizeUi,
    chapterId: Long?,
    onPageLoad: (Int) -> Unit,
    onPageClick: ((Long, Int) -> Unit)?,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = previewGridColumnCount(size, maxWidth)
        val rows = pages.chunked(columns)

        Column(verticalArrangement = Arrangement.spacedBy(PreviewGridSpacing)) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PreviewGridSpacing),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { previewPage ->
                        EntryPreviewTile(
                            previewPage = previewPage,
                            chapterId = chapterId,
                            modifier = Modifier.weight(1f),
                            onPageLoad = onPageLoad,
                            onPageClick = onPageClick,
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryPreviewTile(
    previewPage: EntryViewModel.PreviewPage,
    chapterId: Long?,
    onPageLoad: (Int) -> Unit,
    onPageClick: ((Long, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val status by previewPage.page.status.collectAsState()
    val progress by previewPage.page.progress.collectAsState()
    val progressiveImageFlow = remember(previewPage.page.progressiveImageState) {
        previewPage.page.progressiveImageState ?: flowOf<ProgressiveImageState?>(null)
    }
    val progressiveImage by progressiveImageFlow.collectAsState(initial = null)
    val progressiveBitmap = rememberProgressiveEntryPreviewBitmap(progressiveImage)
    val keepProgressiveAnimation = progressiveImage?.animation?.let {
        it.isComplete && it.isReplayable
    } == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PREVIEW_PAGE_RATIO)
            .border(KatariTheme.sizes.border, KatariTheme.colors.outlineVariant, KatariTheme.shapes.medium)
            .clickable(enabled = onPageClick != null && previewPage.page.isOpenable(chapterId)) {
                chapterId
                    ?.takeIf { previewPage.page.canOpen }
                    ?.let { onPageClick?.invoke(it, previewPage.page.index) }
            },
        contentAlignment = Alignment.Center,
    ) {
        progressiveBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_PAGE_RATIO),
            )
        }
        when (status) {
            EntryPreviewPageStatus.Ready -> {
                if (!keepProgressiveAnimation) {
                    EntryPreviewImage(page = previewPage.page)
                }
            }
            EntryPreviewPageStatus.Queued,
            EntryPreviewPageStatus.LoadingPage,
            EntryPreviewPageStatus.DownloadingImage,
            -> {
                LaunchedEffect(previewPage.page.index) {
                    onPageLoad(previewPage.page.index)
                }
                if (progressiveBitmap == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    ) {
                        if (status == EntryPreviewPageStatus.DownloadingImage && progress in 0..100) {
                            CircularProgressIndicator(progress = { progress / 100f })
                        } else {
                            LoadingIndicator()
                        }
                        Text(
                            text = when (status) {
                                EntryPreviewPageStatus.DownloadingImage -> stringResource(MR.strings.ext_downloading)
                                else -> stringResource(MR.strings.loading)
                            },
                            role = TextRole.Supporting,
                        )
                    }
                } else if (status == EntryPreviewPageStatus.DownloadingImage) {
                    CircularProgressIndicator(
                        progress = { (progress.coerceIn(0, 100)) / 100f },
                        modifier = Modifier.size(KatariTheme.sizes.iconLarge),
                    )
                }
            }
            is EntryPreviewPageStatus.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.BrokenImage,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(MR.strings.chapter_error),
                        role = TextRole.Supporting,
                    )
                }
            }
        }
    }
}

internal fun EntryPreviewPage.isOpenable(chapterId: Long?): Boolean {
    return canOpen && chapterId != null
}

@Composable
private fun EntryPreviewImage(
    page: EntryPreviewPage,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(page.imageModel)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { page.onFinalImageReady() },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PREVIEW_PAGE_RATIO),
    )
}

private const val PREVIEW_PAGE_RATIO = 0.72f
