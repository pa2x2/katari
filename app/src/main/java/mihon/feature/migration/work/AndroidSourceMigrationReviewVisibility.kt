package mihon.feature.migration.work

import android.app.Application
import dev.zacsweers.metro.Inject
import mihon.feature.migration.session.model.SourceMigrationSessionId

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
