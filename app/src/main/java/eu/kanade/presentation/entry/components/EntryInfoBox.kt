package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Brush
import mihon.icons.materialsymbols.rounded.Person
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.content.EntryMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.ArtworkBackdrop
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** An entry's header: its cover beside (phones) or above (tablets) its title, people, status and source. */
@Composable
fun EntryInfoBox(
    isTabletUi: Boolean,
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
) {
    val coverType = remember(entry.source) {
        Injekt.get<EntryCatalogueFeature>().description(entry.source).itemOrientation.toGridCoverType()
    }

    Box(modifier = modifier) {
        ArtworkBackdrop(modifier = Modifier.matchParentSize()) { imageModifier ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(entry)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        }

        CompositionLocalProvider(LocalContentColor provides KatariTheme.colors.onSurface) {
            if (!isTabletUi) {
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
            modifier = Modifier.fillMaxWidth(TABLET_COVER_WIDTH_FRACTION),
            data = ImageRequest.Builder(LocalContext.current)
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        coverType(
            modifier = Modifier
                .sizeIn(
                    maxWidth = if (coverType == EntryCover.Wide) {
                        EntryMetrics.headerWideCoverMaxWidth
                    } else {
                        EntryMetrics.headerCoverMaxWidth
                    },
                )
                .align(Alignment.Top),
            data = ImageRequest.Builder(LocalContext.current)
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
    val context = LocalContext.current
    val statusPresentation = status.presentation()
    val spacing = KatariTheme.spacing
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        role = TextRole.ScreenTitle,
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
                context.copyToClipboard(
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
            onLongClick = { context.copyToClipboard(artist, artist) },
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
