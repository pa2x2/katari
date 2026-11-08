package app.katari.sourcemigration.work

import app.katari.core.common.platform.PlatformFacility
import app.katari.sourcemigration.session.model.SourceMigrationSessionId

/**
 * Tells a Source Migration session's background work when the session's review screen is in front, so its progress
 * shows there rather than in notifications, and the notifications it already posted are taken down.
 */
@PlatformFacility
interface SourceMigrationReviewVisibility {
    fun shown(sessionId: SourceMigrationSessionId)

    fun hidden(sessionId: SourceMigrationSessionId)
}
