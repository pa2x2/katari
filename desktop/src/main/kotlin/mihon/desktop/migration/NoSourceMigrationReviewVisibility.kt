package mihon.desktop.migration

import dev.zacsweers.metro.Inject
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.work.SourceMigrationReviewVisibility

/**
 * Desktop's migration work posts no notifications yet, so none waits on whether a session's review is in front. Stands
 * in until desktop posts notifications.
 */
@Inject
class NoSourceMigrationReviewVisibility : SourceMigrationReviewVisibility {
    override fun shown(sessionId: SourceMigrationSessionId) = Unit

    override fun hidden(sessionId: SourceMigrationSessionId) = Unit
}
