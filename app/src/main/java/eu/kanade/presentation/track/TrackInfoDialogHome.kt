package eu.kanade.presentation.track

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.util.lang.toJavaLocalDate
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import mihon.entry.interactions.tracking.EntryTrackingSessionService
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.VisibilityOff
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.VerticalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter

@Composable
fun TrackInfoDialogHome(
    trackItems: List<EntryTrackingSessionService>,
    dateFormat: DateTimeFormatter,
    onStatusClick: (EntryTrackingSessionService) -> Unit,
    onChapterClick: (EntryTrackingSessionService) -> Unit,
    onScoreClick: (EntryTrackingSessionService) -> Unit,
    onStartDateEdit: (EntryTrackingSessionService) -> Unit,
    onEndDateEdit: (EntryTrackingSessionService) -> Unit,
    onNewSearch: (EntryTrackingSessionService) -> Unit,
    onOpenInBrowser: (EntryTrackingSessionService) -> Unit,
    onRemoved: (EntryTrackingSessionService) -> Unit,
    onCopyLink: (EntryTrackingSessionService) -> Unit,
    onTogglePrivate: (EntryTrackingSessionService) -> Unit,
) {
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(KatariTheme.spacing.contentInset)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xl),
    ) {
        trackItems.forEach { item ->
            val track = item.track
            if (track != null) {
                val supportsScoring = item.service.capabilities.scores.isNotEmpty()
                val supportsReadingDates = item.service.capabilities.supportsReadingDates
                val supportsPrivate = item.service.capabilities.supportsPrivateTracking
                TrackInfoItem(
                    title = track.title,
                    service = item.service,
                    status = item.service.capabilities.statuses
                        .firstOrNull { status -> status.value == track.status }
                        ?.label,
                    onStatusClick = { onStatusClick(item) },
                    chapters = "${track.progress.toInt()}".let {
                        val total = track.total
                        if (total > 0) {
                            // Add known total chapter count
                            "$it / $total"
                        } else {
                            it
                        }
                    },
                    onChaptersClick = { onChapterClick(item) },
                    score = item.displayScore
                        .takeIf { supportsScoring && track.score != 0.0 },
                    onScoreClick = { onScoreClick(item) }
                        .takeIf { supportsScoring },
                    startDate = remember(track.startDate, dateFormat) {
                        dateFormat.format(track.startDate.toJavaLocalDate())
                    }
                        .takeIf { supportsReadingDates && track.startDate != 0L },
                    onStartDateClick = { onStartDateEdit(item) } // TODO
                        .takeIf { supportsReadingDates },
                    endDate = remember(track.finishDate, dateFormat) {
                        dateFormat.format(track.finishDate.toJavaLocalDate())
                    }
                        .takeIf { supportsReadingDates && track.finishDate != 0L },
                    onEndDateClick = { onEndDateEdit(item) }
                        .takeIf { supportsReadingDates },
                    onNewSearch = { onNewSearch(item) },
                    onOpenInBrowser = { onOpenInBrowser(item) },
                    onRemoved = { onRemoved(item) },
                    onCopyLink = { onCopyLink(item) },
                    private = track.private,
                    onTogglePrivate = { onTogglePrivate(item) }
                        .takeIf { supportsPrivate },
                )
            } else {
                TrackInfoItemEmpty(
                    service = item.service,
                    onNewSearch = { onNewSearch(item) },
                )
            }
        }
    }
}

@Composable
private fun TrackInfoItem(
    title: String,
    service: EntryTrackingServiceDescriptor,
    status: StringResource?,
    onStatusClick: () -> Unit,
    chapters: String,
    onChaptersClick: () -> Unit,
    score: String?,
    onScoreClick: (() -> Unit)?,
    startDate: String?,
    onStartDateClick: (() -> Unit)?,
    endDate: String?,
    onEndDateClick: (() -> Unit)?,
    onNewSearch: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    val context = LocalContext.current
    val privateDescription = stringResource(MR.strings.tracked_privately)
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TrackLogoIcon(
                    name = service.name,
                    logoResource = service.logoResource,
                    onClick = onOpenInBrowser,
                    onLongClick = onCopyLink,
                )
                if (private) {
                    BadgeGroup(modifier = Modifier.align(Alignment.TopEnd)) {
                        Badge(
                            icon = MaterialSymbols.RoundedFilled.VisibilityOff,
                            modifier = Modifier.semantics {
                                contentDescription = privateDescription
                            },
                            tone = BadgeTone.Primary,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .height(KatariTheme.sizes.touchTarget)
                    .weight(1f)
                    .combinedClickable(
                        onClick = onNewSearch,
                        onLongClick = {
                            context.copyToClipboard(title, title)
                        },
                    )
                    .padding(start = KatariTheme.spacing.l),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    role = TextRole.SectionTitle,
                )
            }
            VerticalDivider()
            TrackInfoItemMenu(
                onOpenInBrowser = onOpenInBrowser,
                onRemoved = onRemoved,
                onCopyLink = onCopyLink,
                private = private,
                onTogglePrivate = onTogglePrivate,
            )
        }

        Surface(
            modifier = Modifier.padding(top = KatariTheme.spacing.m),
            container = SurfaceContainer.Highest,
            shape = KatariTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .padding(KatariTheme.spacing.s)
                    .clip(KatariTheme.shapes.small),
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        text = status?.let { stringResource(it) } ?: "",
                        onClick = onStatusClick,
                    )
                    VerticalDivider()
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        text = chapters,
                        onClick = onChaptersClick,
                    )
                    if (onScoreClick != null) {
                        VerticalDivider()
                        TrackDetailsItem(
                            modifier = Modifier.weight(1f),
                            text = score,
                            placeholder = stringResource(MR.strings.score),
                            onClick = onScoreClick,
                        )
                    }
                }

                if (onStartDateClick != null && onEndDateClick != null) {
                    HorizontalDivider()
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        TrackDetailsItem(
                            modifier = Modifier.weight(1F),
                            text = startDate,
                            placeholder = stringResource(MR.strings.track_started_reading_date),
                            onClick = onStartDateClick,
                        )
                        VerticalDivider()
                        TrackDetailsItem(
                            modifier = Modifier.weight(1F),
                            text = endDate,
                            placeholder = stringResource(MR.strings.track_finished_reading_date),
                            onClick = onEndDateClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackDetailsItem(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxHeight()
            .padding(KatariTheme.spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text ?: placeholder,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            role = TextRole.Body,
            tone = if (text == null) ContentTone.Secondary else ContentTone.Inherit,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrackInfoItemEmpty(
    service: EntryTrackingServiceDescriptor,
    onNewSearch: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackLogoIcon(name = service.name, logoResource = service.logoResource)
        Button(
            label = stringResource(MR.strings.add_tracking),
            onClick = onNewSearch,
            modifier = Modifier
                .padding(start = KatariTheme.spacing.l)
                .weight(1f),
            style = ButtonStyle.Text,
        )
    }
}

@Composable
private fun TrackInfoItemMenu(
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    OverflowMenu { close ->
        DropdownMenuItem(
            label = stringResource(MR.strings.action_open_in_browser),
            onClick = {
                onOpenInBrowser()
                close()
            },
        )
        DropdownMenuItem(
            label = stringResource(MR.strings.action_copy_link),
            onClick = {
                onCopyLink()
                close()
            },
        )
        if (onTogglePrivate != null) {
            DropdownMenuItem(
                label = stringResource(
                    if (private) MR.strings.action_toggle_private_off else MR.strings.action_toggle_private_on,
                ),
                onClick = {
                    onTogglePrivate()
                    close()
                },
            )
        }
        DropdownMenuItem(
            label = stringResource(MR.strings.action_remove),
            onClick = {
                onRemoved()
                close()
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackInfoDialogHomePreviews(
    @PreviewParameter(TrackInfoDialogHomePreviewProvider::class)
    content: @Composable () -> Unit,
) {
    KatariPreviewTheme {
        Surface {
            content()
        }
    }
}
