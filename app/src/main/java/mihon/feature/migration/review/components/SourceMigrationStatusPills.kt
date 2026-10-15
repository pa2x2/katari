package mihon.feature.migration.review.components

import androidx.compose.runtime.Composable
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.presentation.kit.surface.StatusPill
import mihon.presentation.kit.surface.StatusTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Where one entry's migration stands, from the search for a replacement through to the migration itself. */
@Composable
internal fun SourceMigrationItemStatusPill(state: SourceMigrationItemState) {
    val label = when (state) {
        SourceMigrationItemState.DISCOVERY_QUEUED,
        SourceMigrationItemState.DISCOVERING,
        -> stringResource(MR.strings.sourceMigrationReview_searching)
        SourceMigrationItemState.READY -> stringResource(MR.strings.sourceMigrationReview_ready)
        SourceMigrationItemState.NEEDS_REVIEW,
        SourceMigrationItemState.CONFLICT,
        -> stringResource(MR.strings.sourceMigrationReview_needsReview)
        SourceMigrationItemState.NO_MATCH -> stringResource(MR.strings.sourceMigrationReview_noMatchStatus)
        SourceMigrationItemState.DISCOVERY_FAILED,
        SourceMigrationItemState.EXECUTION_FAILED,
        SourceMigrationItemState.CANCELLED,
        -> stringResource(MR.strings.sourceMigrationReview_failed)
        SourceMigrationItemState.EXECUTION_QUEUED,
        SourceMigrationItemState.EXECUTING,
        -> stringResource(MR.strings.sourceMigrationReview_migrating)
        SourceMigrationItemState.APPLIED -> stringResource(MR.strings.sourceMigrationReview_migrated)
        SourceMigrationItemState.APPLIED_INCOMPLETE -> stringResource(MR.strings.sourceMigrationReview_incomplete)
    }
    val tone = when (state) {
        SourceMigrationItemState.READY,
        SourceMigrationItemState.APPLIED,
        -> StatusTone.Ready
        SourceMigrationItemState.DISCOVERY_QUEUED,
        SourceMigrationItemState.DISCOVERING,
        SourceMigrationItemState.EXECUTION_QUEUED,
        SourceMigrationItemState.EXECUTING,
        -> StatusTone.Checking
        SourceMigrationItemState.NEEDS_REVIEW,
        SourceMigrationItemState.CONFLICT,
        SourceMigrationItemState.NO_MATCH,
        -> StatusTone.ActionRequired
        SourceMigrationItemState.DISCOVERY_FAILED,
        SourceMigrationItemState.EXECUTION_FAILED,
        SourceMigrationItemState.CANCELLED,
        SourceMigrationItemState.APPLIED_INCOMPLETE,
        -> StatusTone.Unavailable
    }
    StatusPill(text = label, tone = tone)
}

/** Whether the search for one entry's replacement is running now or still waiting its turn. */
@Composable
internal fun SourceMigrationSearchStatusPill(state: SourceMigrationItemState) {
    StatusPill(
        text = stringResource(
            if (state == SourceMigrationItemState.DISCOVERING) {
                MR.strings.sourceMigrationReview_searchingNow
            } else {
                MR.strings.sourceMigrationReview_queued
            },
        ),
        tone = StatusTone.Checking,
    )
}

/** Where a whole group stands while replacements are searched for: still searching, some without a match, or ready. */
@Composable
internal fun SourceMigrationGroupStatusPill(searching: Boolean, noMatchCount: Int) {
    when {
        searching -> StatusPill(stringResource(MR.strings.sourceMigrationReview_searching), StatusTone.Checking)
        noMatchCount > 0 -> StatusPill(
            text = stringResource(MR.strings.sourceMigrationReview_noMatch, noMatchCount),
            tone = StatusTone.ActionRequired,
        )
        else -> StatusPill(stringResource(MR.strings.sourceMigrationReview_ready), StatusTone.Ready)
    }
}
