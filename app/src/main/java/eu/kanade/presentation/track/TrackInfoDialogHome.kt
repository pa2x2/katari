package eu.kanade.presentation.track

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.util.lang.toJavaLocalDate
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import mihon.entry.interactions.tracking.EntryTrackingSessionService
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Link
import mihon.icons.materialsymbols.rounded.LockPerson
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.roundedfilled.VisibilityOff
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.menu.DropdownMenuDivider
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.InfoTile
import mihon.presentation.kit.surface.InfoTileAction
import mihon.presentation.kit.surface.InfoTileRow
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
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
    val (linked, unlinked) = trackItems.partition { it.track != null }
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = KatariTheme.spacing.l),
    ) {
        SheetHeader(title = stringResource(MR.strings.track))
        linked.forEach { item ->
            val track = item.track!!
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
                    if (total > 0) "$it / $total" else it
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
                onStartDateClick = { onStartDateEdit(item) }
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
            RowGroupGap()
        }
        if (unlinked.isNotEmpty()) {
            RowGroup(
                rows = unlinked.map<EntryTrackingSessionService, @Composable () -> Unit> { item ->
                    { TrackInfoItemEmpty(service = item.service, onNewSearch = { onNewSearch(item) }) }
                },
            )
        }
    }
}

/** A linked tracker as one card: its logo, title and menu, then its details as tiles that each open their editor. */
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
    val unset = stringResource(MR.strings.track_value_unset)
    RowGroup(
        rows = listOf<@Composable () -> Unit> {
            Column(modifier = Modifier.padding(bottom = KatariTheme.spacing.m)) {
                SettingsRow(
                    title = title,
                    leading = {
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
                    },
                    trailing = {
                        TrackInfoItemMenu(
                            onOpenInBrowser = onOpenInBrowser,
                            onRemoved = onRemoved,
                            onCopyLink = onCopyLink,
                            private = private,
                            onTogglePrivate = onTogglePrivate,
                        )
                    },
                    onClick = onNewSearch,
                    onLongClick = { context.copyToClipboard(title, title) },
                )
                Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                    InfoTileRow {
                        InfoTile(
                            value = status?.let { stringResource(it) } ?: unset,
                            label = stringResource(MR.strings.track_status),
                            action = InfoTileAction.Edit,
                            onClick = onStatusClick,
                        )
                        InfoTile(
                            value = chapters,
                            label = stringResource(MR.strings.track_progress),
                            action = InfoTileAction.Edit,
                            onClick = onChaptersClick,
                        )
                        if (onScoreClick != null) {
                            InfoTile(
                                value = score ?: unset,
                                label = stringResource(MR.strings.score),
                                action = InfoTileAction.Edit,
                                onClick = onScoreClick,
                            )
                        }
                    }
                    if (onStartDateClick != null && onEndDateClick != null) {
                        InfoTileRow {
                            InfoTile(
                                value = startDate ?: unset,
                                label = stringResource(MR.strings.track_started_reading_date),
                                action = InfoTileAction.Edit,
                                onClick = onStartDateClick,
                            )
                            InfoTile(
                                value = endDate ?: unset,
                                label = stringResource(MR.strings.track_finished_reading_date),
                                action = InfoTileAction.Edit,
                                onClick = onEndDateClick,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun TrackInfoItemEmpty(
    service: EntryTrackingServiceDescriptor,
    onNewSearch: () -> Unit,
) {
    SettingsRow(
        title = service.name,
        leading = { TrackLogoIcon(name = service.name, logoResource = service.logoResource) },
        trailing = {
            Button(
                label = stringResource(MR.strings.add_tracking),
                onClick = onNewSearch,
                style = ButtonStyle.Text,
            )
        },
        onClick = onNewSearch,
    )
}

@Composable
private fun TrackInfoItemMenu(
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    OverflowMenu {
        DropdownMenuItem(
            label = stringResource(MR.strings.action_open_in_browser),
            onClick = onOpenInBrowser,
            icon = MaterialSymbols.Rounded.OpenInBrowser,
        )
        DropdownMenuItem(
            label = stringResource(MR.strings.action_copy_link),
            onClick = onCopyLink,
            icon = MaterialSymbols.Rounded.Link,
        )
        if (onTogglePrivate != null) {
            DropdownMenuItem(
                label = stringResource(
                    if (private) MR.strings.action_toggle_private_off else MR.strings.action_toggle_private_on,
                ),
                onClick = onTogglePrivate,
                icon = MaterialSymbols.Rounded.LockPerson,
            )
        }
        DropdownMenuDivider()
        DropdownMenuItem(
            label = stringResource(MR.strings.action_remove),
            onClick = onRemoved,
            icon = MaterialSymbols.Rounded.Delete,
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
