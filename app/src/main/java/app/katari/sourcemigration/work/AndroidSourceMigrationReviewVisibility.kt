package app.katari.sourcemigration.work

import android.app.Application
import app.katari.sourcemigration.session.model.SourceMigrationSessionId
import dev.zacsweers.metro.Inject

/** Holds back the workers' progress notifications while the review screen shows, and clears the ones they posted. */
@Inject
class AndroidSourceMigrationReviewVisibility(private val application: Application) : SourceMigrationReviewVisibility {
    override fun shown(sessionId: SourceMigrationSessionId) {
        SourceMigrationNotificationVisibility.show(sessionId)
        SourceMigrationNotifier(application).cancel(sessionId)
    }

    override fun hidden(sessionId: SourceMigrationSessionId) {
        SourceMigrationNotificationVisibility.hide(sessionId)
    }
}
