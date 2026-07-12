package eu.kanade.presentation.entry.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.DISALLOWED_MARKDOWN_TYPES
import eu.kanade.presentation.components.DotSeparatorText
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.MARKDOWN_INLINE_IMAGE_TAG
import eu.kanade.presentation.components.MarkdownRender
import eu.kanade.presentation.components.getMarkdownLinkStyle
import eu.kanade.presentation.entry.components.EntryNotesSection
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.sourceItemOrientation
import eu.kanade.tachiyomi.ui.entry.EntryScreenModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.flow.collectLatest
import mihon.entry.interactions.EntryPreviewPage
import mihon.entry.interactions.EntryPreviewPageStatus
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun EntryInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    entry: Entry,
    sourceName: String,
    isStubSource: Boolean,
    mergedMemberTitles: List<String>,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverType = remember(entry.source) {
        Injekt.get<SourceManager>().getOrStub(entry.source).sourceItemOrientation().toGridCoverType()
    }

    Box(modifier = modifier) {
        // Backdrop
        val backdropGradientColors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.background,
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(entry)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(colors = backdropGradientColors),
                    )
                }
                .blur(4.dp)
                .alpha(0.2f),
        )

        // Manga & source info
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (!isTabletUi) {
                EntryAndSourceTitlesSmall(
                    appBarPadding = appBarPadding,
                    entry = entry,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    mergedMemberTitles = mergedMemberTitles,
                    coverType = coverType,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                )
            } else {
                EntryAndSourceTitlesLarge(
                    appBarPadding = appBarPadding,
                    entry = entry,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    mergedMemberTitles = mergedMemberTitles,
                    coverType = coverType,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                )
            }
        }
    }
}

@Composable
fun EntryActionRow(
    favorite: Boolean,
    trackingCount: Int,
    nextUpdate: Instant?,
    isUserIntervalMode: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onDuplicatesClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onEditCategory: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)

    // TODO: show something better when using custom interval
    val nextUpdateDays = remember(nextUpdate) {
        return@remember if (nextUpdate != null) {
            val now = Instant.now()
            now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
        } else {
            null
        }
    }

    Row(modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        EntryActionButton(
            title = if (favorite) {
                stringResource(MR.strings.in_library)
            } else {
                stringResource(MR.strings.add_to_library)
            },
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            color = if (favorite) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
            onClick = onAddToLibraryClicked,
            onLongClick = onEditCategory,
        )
        if (onAddToMergeClicked != null) {
            EntryActionButton(
                title = stringResource(MR.strings.action_add_to_merge),
                icon = Icons.AutoMirrored.Outlined.CallSplit,
                color = defaultActionButtonColor,
                onClick = onAddToMergeClicked,
            )
        }
        EntryActionButton(
            title = when (nextUpdateDays) {
                null -> stringResource(MR.strings.not_applicable)
                0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
                else -> pluralStringResource(
                    MR.plurals.day,
                    count = nextUpdateDays,
                    nextUpdateDays,
                )
            },
            icon = Icons.Default.HourglassEmpty,
            color = if (isUserIntervalMode) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
            onClick = { onEditIntervalClicked?.invoke() },
        )
        EntryActionButton(
            title = if (trackingCount == 0) {
                stringResource(MR.strings.manga_tracking_tab)
            } else {
                pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
            },
            icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.Done,
            color = if (trackingCount == 0) defaultActionButtonColor else MaterialTheme.colorScheme.primary,
            onClick = onTrackingClicked,
        )
        if (onWebViewClicked != null) {
            EntryActionButton(
                title = stringResource(MR.strings.action_web_view),
                icon = Icons.Outlined.Public,
                color = defaultActionButtonColor,
                onClick = onWebViewClicked,
                onLongClick = onWebViewLongClicked,
            )
        }
        if (onDuplicatesClicked != null) {
            EntryActionButton(
                title = stringResource(MR.strings.action_duplicates),
                icon = Icons.Filled.Warning,
                color = MaterialTheme.colorScheme.error,
                onClick = onDuplicatesClicked,
            )
        }
    }
}

@Composable
fun ExpandableEntryDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onEditNotes: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: PreviewSizeUi,
    entryPreviewState: EntryScreenModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

        EntrySummary(
            description = desc,
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .clickableNoIndication { onExpanded(!expanded) },
        )
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(vertical = 12.dp)
                    .animateContentSize(animationSpec = spring())
                    .fillMaxWidth(),
            ) {
                var showMenu by remember { mutableStateOf(false) }
                var tagSelected by remember { mutableStateOf("") }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_search)) },
                        onClick = {
                            onTagSearch(tagSelected)
                            showMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
                        onClick = {
                            onCopyTagToClipboard(tagSelected)
                            showMenu = false
                        },
                    )
                }
                if (expanded) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        tags.forEach {
                            TagsChip(
                                modifier = DefaultTagChipModifier,
                                text = it,
                                onClick = {
                                    tagSelected = it
                                    showMenu = true
                                },
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        items(items = tags) {
                            TagsChip(
                                modifier = DefaultTagChipModifier,
                                text = it,
                                onClick = {
                                    tagSelected = it
                                    showMenu = true
                                },
                            )
                        }
                    }
                }
            }
        }
        if (entryPreviewEnabled) {
            SharedEntryPreviewSection(
                state = entryPreviewState,
                size = entryPreviewSize,
                onExpandedChange = onPreviewExpandedChange,
                onRetry = onPreviewRetry,
                onPageLoad = onPreviewPageLoad,
                onPageClick = onPreviewPageClick,
            )
        }
    }
}

enum class PreviewSizeUi {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
}

private val PreviewGridSpacing = 12.dp

internal fun previewGridColumnCount(
    size: PreviewSizeUi,
    availableWidth: Dp,
): Int {
    val preferredTileWidth = when (size) {
        PreviewSizeUi.SMALL -> 88.dp
        PreviewSizeUi.MEDIUM -> 112.dp
        PreviewSizeUi.LARGE -> 140.dp
        // Matches the previous 560/840 dp focused-mode breakpoints with spacing-aware math.
        PreviewSizeUi.EXTRA_LARGE -> 272.dp
    }

    return ((availableWidth + PreviewGridSpacing) / (preferredTileWidth + PreviewGridSpacing))
        .toInt()
        .coerceAtLeast(1)
}

@Composable
fun SharedEntryPreviewSection(
    state: EntryScreenModel.EntryPreviewState,
    size: PreviewSizeUi,
    onExpandedChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onPageLoad: (Int) -> Unit,
    onPageClick: (Long, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable { onExpandedChange(!state.isExpanded) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.manga_preview_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)
            Icon(
                painter = rememberAnimatedVectorPainter(image, !state.isExpanded),
                contentDescription = stringResource(
                    if (state.isExpanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                ),
            )
        }

        if (state.isExpanded) {
            PreviewContent(
                state = state,
                size = size,
                onRetry = onRetry,
                onPageLoad = onPageLoad,
                onPageClick = onPageClick,
            )
        }
    }
}

@Composable
fun PreviewContent(
    state: EntryScreenModel.EntryPreviewState,
    size: PreviewSizeUi,
    onRetry: () -> Unit,
    onPageLoad: (Int) -> Unit,
    onPageClick: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
    centerStates: Boolean = false,
    loadingContent: @Composable BoxScope.() -> Unit = {
        PreviewMessage(
            icon = Icons.Default.Image,
            text = stringResource(MR.strings.transition_pages_loading),
            modifier = Modifier.align(Alignment.Center),
        )
    },
) {
    Box(modifier = modifier) {
        when {
            state.isLoading && state.pages.isEmpty() -> {
                loadingContent()
            }
            state.error != null -> {
                PreviewError(
                    message = state.error.message ?: stringResource(MR.strings.unknown_error),
                    onRetry = onRetry,
                    modifier = if (centerStates) {
                        Modifier.align(Alignment.Center)
                    } else {
                        Modifier
                    },
                )
            }
            state.pages.isEmpty() -> {
                PreviewMessage(
                    icon = Icons.Default.Warning,
                    text = stringResource(MR.strings.manga_preview_empty),
                    modifier = if (centerStates) {
                        Modifier.align(Alignment.Center)
                    } else {
                        Modifier
                    },
                )
            }
            else -> {
                PreviewGrid(
                    pages = state.pages,
                    size = size,
                    chapterId = state.chapterId,
                    onPageLoad = onPageLoad,
                    onPageClick = onPageClick,
                )
            }
        }
    }
}

@Composable
fun PreviewGrid(
    pages: List<EntryScreenModel.PreviewPage>,
    size: PreviewSizeUi,
    chapterId: Long?,
    onPageLoad: (Int) -> Unit,
    onPageClick: (Long, Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = previewGridColumnCount(size, maxWidth)
        val rows = pages.chunked(columns)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    previewPage: EntryScreenModel.PreviewPage,
    chapterId: Long?,
    onPageLoad: (Int) -> Unit,
    onPageClick: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status by previewPage.page.status.collectAsState()
    val progress by previewPage.page.progress.collectAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .clickable(enabled = previewPage.page.isOpenable(chapterId)) {
                    chapterId
                        ?.takeIf { previewPage.page.canOpen }
                        ?.let { onPageClick(it, previewPage.page.index) }
                },
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                EntryPreviewPageStatus.Ready -> {
                    EntryPreviewImage(
                        page = previewPage.page,
                    )
                }
                EntryPreviewPageStatus.Queued,
                EntryPreviewPageStatus.LoadingPage,
                EntryPreviewPageStatus.DownloadingImage,
                -> {
                    LaunchedEffect(previewPage.page.index) {
                        onPageLoad(previewPage.page.index)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (status == EntryPreviewPageStatus.DownloadingImage && progress in 0..100) {
                            CircularProgressIndicator(progress = { progress / 100f })
                        } else {
                            CircularProgressIndicator()
                        }
                        Text(
                            text = when (status) {
                                EntryPreviewPageStatus.DownloadingImage -> stringResource(MR.strings.ext_downloading)
                                else -> stringResource(MR.strings.loading)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is EntryPreviewPageStatus.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(MR.strings.chapter_error),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f),
    )
}

@Composable
fun PreviewError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewMessage(
            icon = Icons.Default.Warning,
            text = message,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(MR.strings.action_retry))
        }
    }
}

@Composable
fun PreviewMessage(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EntryAndSourceTitlesLarge(
    appBarPadding: Dp,
    entry: Entry,
    sourceName: String,
    isStubSource: Boolean,
    mergedMemberTitles: List<String>,
    coverType: EntryCover,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        coverType(
            modifier = Modifier.fillMaxWidth(0.65f),
            data = ImageRequest.Builder(LocalContext.current)
                .data(entry)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(MR.strings.manga_cover),
            onClick = onCoverClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
        EntryContentInfo(
            title = entry.displayTitle,
            author = entry.author,
            artist = entry.artist,
            status = entry.status,
            sourceName = sourceName,
            isStubSource = isStubSource,
            mergedMemberTitles = mergedMemberTitles,
            doSearch = doSearch,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EntryAndSourceTitlesSmall(
    appBarPadding: Dp,
    entry: Entry,
    sourceName: String,
    isStubSource: Boolean,
    mergedMemberTitles: List<String>,
    coverType: EntryCover,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        coverType(
            modifier = Modifier
                .sizeIn(maxWidth = if (coverType == EntryCover.Wide) 160.dp else 100.dp)
                .align(Alignment.Top),
            data = ImageRequest.Builder(LocalContext.current)
                .data(entry)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(MR.strings.manga_cover),
            onClick = onCoverClick,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            EntryContentInfo(
                title = entry.displayTitle,
                author = entry.author,
                artist = entry.artist,
                status = entry.status,
                sourceName = sourceName,
                isStubSource = isStubSource,
                mergedMemberTitles = mergedMemberTitles,
                doSearch = doSearch,
            )
        }
    }
}

@Composable
private fun ColumnScope.EntryContentInfo(
    title: String,
    author: String?,
    artist: String?,
    status: EntryStatus,
    sourceName: String,
    isStubSource: Boolean,
    mergedMemberTitles: List<String>,
    doSearch: (query: String, global: Boolean) -> Unit,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
) {
    val context = LocalContext.current
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.clickableNoIndication(
            onLongClick = {
                if (title.isNotBlank()) {
                    context.copyToClipboard(
                        title,
                        title,
                    )
                }
            },
            onClick = { if (title.isNotBlank()) doSearch(title, true) },
        ),
        textAlign = textAlign,
    )

    Spacer(modifier = Modifier.height(2.dp))

    if (mergedMemberTitles.size > 1) {
        Text(
            text = mergedMemberTitles.joinToString(separator = " • "),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.secondaryItemAlpha(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
        Spacer(modifier = Modifier.height(2.dp))
    }

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = author?.takeIf { it.isNotBlank() }
                ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .clickableNoIndication(
                    onLongClick = {
                        if (!author.isNullOrBlank()) {
                            context.copyToClipboard(
                                author,
                                author,
                            )
                        }
                    },
                    onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
                ),
            textAlign = textAlign,
        )
    }

    if (!artist.isNullOrBlank() && author != artist) {
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Brush,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .clickableNoIndication(
                        onLongClick = { context.copyToClipboard(artist, artist) },
                        onClick = { doSearch(artist, true) },
                    ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (status) {
                EntryStatus.ONGOING -> Icons.Outlined.Schedule
                EntryStatus.COMPLETED -> Icons.Outlined.DoneAll
                EntryStatus.LICENSED -> Icons.Outlined.AttachMoney
                EntryStatus.PUBLISHING_FINISHED -> Icons.Outlined.Done
                EntryStatus.CANCELLED -> Icons.Outlined.Close
                EntryStatus.ON_HIATUS -> Icons.Outlined.Pause
                else -> Icons.Outlined.Block
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Text(
                text = when (status) {
                    EntryStatus.ONGOING -> stringResource(MR.strings.ongoing)
                    EntryStatus.COMPLETED -> stringResource(MR.strings.completed)
                    EntryStatus.LICENSED -> stringResource(MR.strings.licensed)
                    EntryStatus.PUBLISHING_FINISHED -> stringResource(MR.strings.publishing_finished)
                    EntryStatus.CANCELLED -> stringResource(MR.strings.cancelled)
                    EntryStatus.ON_HIATUS -> stringResource(MR.strings.on_hiatus)
                    else -> stringResource(MR.strings.unknown)
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            DotSeparatorText()
            if (isStubSource) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = sourceName,
                modifier = Modifier.clickableNoIndication {
                    doSearch(
                        sourceName,
                        false,
                    )
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun descriptionAnnotator(loadImages: Boolean, linkStyle: SpanStyle) = remember(loadImages, linkStyle) {
    markdownAnnotator(
        annotate = { content, child ->
            if (!loadImages && child.type == MarkdownElementTypes.IMAGE) {
                val inlineLink = child.findChildOfType(MarkdownElementTypes.INLINE_LINK)

                val url = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getUnescapedTextInNode(content)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.AUTOLINK)
                        ?.findChildOfType(MarkdownTokenTypes.AUTOLINK)
                        ?.getUnescapedTextInNode(content)
                    ?: return@markdownAnnotator false

                val textNode = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TITLE)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TEXT)
                val altText = textNode?.findChildOfType(MarkdownTokenTypes.TEXT)
                    ?.getUnescapedTextInNode(content).orEmpty()

                withLink(LinkAnnotation.Url(url = url)) {
                    pushStyle(linkStyle)
                    appendInlineContent(MARKDOWN_INLINE_IMAGE_TAG)
                    append(altText)
                    pop()
                }

                return@markdownAnnotator true
            }

            if (child.type in DISALLOWED_MARKDOWN_TYPES) {
                append(content.substring(child.startOffset, child.endOffset))
                return@markdownAnnotator true
            }

            false
        },
        config = markdownAnnotatorConfig(
            eolAsNewLine = true,
        ),
    )
}

@Composable
private fun EntrySummary(
    description: String,
    notes: String,
    expanded: Boolean,
    onEditNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember { Injekt.get<UiPreferences>() }
    val loadImages = remember { preferences.imagesInDescription.get() }
    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        label = "summary",
    )
    var infoHeight by remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.clipToBounds(),
        contents = listOf(
            {
                Text(
                    // Shows at least 3 lines if no notes
                    // when there are notes show 6
                    text = if (notes.isBlank()) "\n\n" else "\n\n\n\n\n",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            {
                Column(
                    modifier = Modifier.onSizeChanged { size ->
                        infoHeight = size.height
                    },
                ) {
                    EntryNotesSection(
                        content = notes,
                        expanded = expanded,
                        onEditNotes = onEditNotesClicked,
                    )
                    SelectionContainer {
                        MarkdownRender(
                            content = description,
                            modifier = Modifier.secondaryItemAlpha(),
                            annotator = descriptionAnnotator(
                                loadImages = loadImages,
                                linkStyle = getMarkdownLinkStyle().toSpanStyle(),
                            ),
                            loadImages = loadImages,
                        )
                    }
                }
            },
            {
                val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                Box(
                    modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)
                    Icon(
                        painter = rememberAnimatedVectorPainter(image, !expanded),
                        contentDescription = stringResource(
                            if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                        ),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.background(Brush.radialGradient(colors = colors.asReversed())),
                    )
                }
            },
        ),
    ) { (shrunk, actual, scrim), constraints ->
        val shrunkHeight = shrunk.single()
            .measure(constraints)
            .height
        val heightDelta = infoHeight - shrunkHeight
        val scrimHeight = 24.dp.roundToPx()

        val actualPlaceable = actual.single()
            .measure(constraints)
        val scrimPlaceable = scrim.single()
            .measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight))

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.place(0, 0)

            val scrimY = currentHeight - scrimHeight
            scrimPlaceable.place(0, scrimY)
        }
    }
}

private val DefaultTagChipModifier = Modifier.padding(vertical = 4.dp)

@Composable
private fun TagsChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        SuggestionChip(
            modifier = modifier,
            onClick = onClick,
            label = { Text(text = text, style = MaterialTheme.typography.bodySmall) },
        )
    }
}

@Composable
private fun RowScope.EntryActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        onLongClick = onLongClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
