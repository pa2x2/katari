package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Brush
import app.katari.icons.materialsymbols.rounded.Person
import app.katari.icons.materialsymbols.roundedfilled.Warning
import app.katari.presentation.kit.content.EntryMetrics
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.ArtworkBackdrop
import app.katari.presentation.kit.surface.ContentOn
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.ui.di.LocalAppUiGraph
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.tachiyomi.data.coil.SourceImage
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication

/** An entry's header: its cover above ([coverAbove], a tall pane of its own) or beside its title, people, status and source. */
@Composable
fun EntryInfoBox(
    coverAbove: Boolean,
    appBarPadding: Dp,
    entry: Entry,
    sourceName: String,
    isStubSource: Boolean,
    mergedEntryCount: Int,
    onCoverClick: () -> Unit,
    onSourceClick: () -> Unit,
    onMergedEntriesClick: (() -> Unit)?,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdropBleed: PaddingValues = PaddingValues(),
) {
    val catalogueFeature = LocalAppUiGraph.current.entryCatalogueFeature
    val coverType = remember(entry.source) {
        catalogueFeature.description(entry.source).itemOrientation.toGridCoverType()
    }

    Box(modifier = modifier) {
        ArtworkBackdrop(modifier = Modifier.matchParentSize(), bleed = backdropBleed) { imageModifier ->
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(entry.backgroundUrl?.let { SourceImage(entry.source, it) } ?: entry)
                    // The cover above decodes this image at its own size at the same moment. In a shared memory cache
                    // entry, whichever decode finished last would be what later covers of this entry are drawn from,
                    // such as the merge sheet's, so they would differ from one visit to the next.
                    .memoryCacheKeyExtra(MEMORY_CACHE_PRESENTATION, BACKDROP_PRESENTATION)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        }

        ContentOn(SurfaceContainer.Surface) {
            if (!coverAbove) {
                EntryAndSourceTitlesSmall(
                    appBarPadding = appBarPadding,
                    entry = entry,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    mergedEntryCount = mergedEntryCount,
                    coverType = coverType,
                    onCoverClick = onCoverClick,
                    onSourceClick = onSourceClick,
                    onMergedEntriesClick = onMergedEntriesClick,
                    doSearch = doSearch,
                )
            } else {
                EntryAndSourceTitlesLarge(
                    appBarPadding = appBarPadding,
                    entry = entry,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    mergedEntryCount = mergedEntryCount,
                    coverType = coverType,
                    onCoverClick = onCoverClick,
                    onSourceClick = onSourceClick,
                    onMergedEntriesClick = onMergedEntriesClick,
                    doSearch = doSearch,
                )
            }
        }
    }
}

@Composable
private fun EntryAndSourceTitlesLarge(
    appBarPadding: Dp,
    entry: Entry,
    sourceName: String,
    isStubSource: Boolean,
    mergedEntryCount: Int,
    coverType: EntryCover,
    onCoverClick: () -> Unit,
    onSourceClick: () -> Unit,
    onMergedEntriesClick: (() -> Unit)?,
    doSearch: (query: String, global: Boolean) -> Unit,
) {
    val spacing = KatariTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.contentInset, top = appBarPadding + spacing.l, end = spacing.contentInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        coverType(
            modifier = Modifier
                .fillMaxWidth(TABLET_COVER_WIDTH_FRACTION),
            raised = true,
            data = ImageRequest.Builder(LocalPlatformContext.current)
                .data(entry)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(MR.strings.manga_cover),
            onClick = onCoverClick,
        )
        Spacer(modifier = Modifier.height(spacing.l))
        EntryContentInfo(
            title = entry.displayTitle,
            author = entry.author,
            artist = entry.artist,
            status = entry.status,
            sourceName = sourceName,
            isStubSource = isStubSource,
            mergedEntryCount = mergedEntryCount,
            onSourceClick = onSourceClick,
            onMergedEntriesClick = onMergedEntriesClick,
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
    mergedEntryCount: Int,
    coverType: EntryCover,
    onCoverClick: () -> Unit,
    onSourceClick: () -> Unit,
    onMergedEntriesClick: (() -> Unit)?,
    doSearch: (query: String, global: Boolean) -> Unit,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.contentInset, top = appBarPadding + spacing.l, end = spacing.contentInset),
        horizontalArrangement = Arrangement.spacedBy(spacing.l),
        verticalAlignment = Alignment.Bottom,
    ) {
        coverType(
            modifier = Modifier
                .sizeIn(
                    maxWidth = if (coverType == EntryCover.Wide) {
                        EntryMetrics.headerWideCoverMaxWidth
                    } else {
                        EntryMetrics.headerCoverMaxWidth
                    },
                ),
            raised = true,
            data = ImageRequest.Builder(LocalPlatformContext.current)
                .data(entry)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(MR.strings.manga_cover),
            onClick = onCoverClick,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            EntryContentInfo(
                title = entry.displayTitle,
                author = entry.author,
                artist = entry.artist,
                status = entry.status,
                sourceName = sourceName,
                isStubSource = isStubSource,
                mergedEntryCount = mergedEntryCount,
                onSourceClick = onSourceClick,
                onMergedEntriesClick = onMergedEntriesClick,
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
    mergedEntryCount: Int,
    onSourceClick: () -> Unit,
    onMergedEntriesClick: (() -> Unit)?,
    doSearch: (query: String, global: Boolean) -> Unit,
    textAlign: TextAlign? = null,
) {
    val textClipboard = LocalAppUiGraph.current.textClipboard
    val statusPresentation = status.presentation()
    val spacing = KatariTheme.spacing
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        role = TextRole.DetailTitle,
        modifier = Modifier.clickableNoIndication(
            onLongClick = {
                if (title.isNotBlank()) {
                    textClipboard.copy(
                        title,
                        title,
                    )
                }
            },
            onClick = { if (title.isNotBlank()) doSearch(title, true) },
        ),
        textAlign = textAlign,
    )

    Spacer(modifier = Modifier.height(spacing.xxs))

    if (mergedEntryCount > 1 && onMergedEntriesClick != null) {
        MergedEntriesLink(count = mergedEntryCount, onClick = onMergedEntriesClick)
        Spacer(modifier = Modifier.height(spacing.xxs))
    }

    EntryPersonLine(
        icon = MaterialSymbols.Rounded.Person,
        name = author?.takeIf { it.isNotBlank() } ?: stringResource(MR.strings.unknown_author),
        onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
        onLongClick = {
            if (!author.isNullOrBlank()) {
                textClipboard.copy(
                    author,
                    author,
                )
            }
        },
        textAlign = textAlign,
    )

    if (!artist.isNullOrBlank() && author != artist) {
        EntryPersonLine(
            icon = MaterialSymbols.Rounded.Brush,
            name = artist,
            onClick = { doSearch(artist, true) },
            onLongClick = { textClipboard.copy(artist, artist) },
            textAlign = textAlign,
        )
    }

    Spacer(modifier = Modifier.height(spacing.xxs))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = statusPresentation.icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = spacing.xs)
                .size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Secondary,
        )
        Text(
            text = statusPresentation.label,
            role = TextRole.Body,
            tone = ContentTone.Secondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        Text(text = DOT_SEPARATOR, role = TextRole.Body, tone = ContentTone.Secondary)
        if (isStubSource) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.Warning,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = spacing.xs)
                    .size(KatariTheme.sizes.iconSmall),
                tone = ContentTone.Error,
            )
        }
        Text(
            text = sourceName,
            modifier = Modifier.clickableNoIndication(onClick = onSourceClick),
            role = TextRole.Body,
            tone = ContentTone.Secondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun EntryPersonLine(
    icon: ImageVector,
    name: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    textAlign: TextAlign?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Secondary,
        )
        Text(
            text = name,
            role = TextRole.Body,
            tone = ContentTone.Secondary,
            modifier = Modifier.clickableNoIndication(onLongClick = onLongClick, onClick = onClick),
            textAlign = textAlign,
        )
    }
}

private const val TABLET_COVER_WIDTH_FRACTION = 0.65f
private const val DOT_SEPARATOR = " • "
private const val MEMORY_CACHE_PRESENTATION = "presentation"
private const val BACKDROP_PRESENTATION = "backdrop"
