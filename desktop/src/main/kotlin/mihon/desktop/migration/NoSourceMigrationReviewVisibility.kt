package mihon.desktop.migration

import dev.zacsweers.metro.Inject
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.work.SourceMigrationReviewVisibility

/**
 * Desktop runs no background migration work, so no notification waits on whether a session's review is in front.
 * Stands in until desktop hosts that work.
 */
@Inject
class NoSourceMigrationReviewVisibility : SourceMigrationReviewVisibility {
    override fun shown(sessionId: SourceMigrationSessionId) = Unit

    override fun hidden(sessionId: SourceMigrationSessionId) = Unit
}
