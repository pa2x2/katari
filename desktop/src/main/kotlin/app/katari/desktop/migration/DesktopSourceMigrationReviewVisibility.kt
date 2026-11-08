package app.katari.desktop.migration

import app.katari.sourcemigration.session.model.SourceMigrationSessionId
import app.katari.sourcemigration.work.SourceMigrationReviewVisibility
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Which sessions' review screens show, counted, as more than one screen can show the same session. While one shows,
 * [DesktopSourceMigrationNotifier] posts nothing for its session, and the review takes down what it posted before.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopSourceMigrationReviewVisibility(
    // The notifier asks this before each notification
    private val notifier: Lazy<DesktopSourceMigrationNotifier>,
) : SourceMigrationReviewVisibility {
    private val shownCounts = mutableMapOf<SourceMigrationSessionId, Int>()

    override fun shown(sessionId: SourceMigrationSessionId) {
        synchronized(shownCounts) { shownCounts[sessionId] = shownCounts.getOrDefault(sessionId, 0) + 1 }
        notifier.value.withdraw(sessionId)
    }

    override fun hidden(sessionId: SourceMigrationSessionId) {
        synchronized(shownCounts) {
            val remaining = shownCounts.getOrDefault(sessionId, 0) - 1
            if (remaining > 0) shownCounts[sessionId] = remaining else shownCounts.remove(sessionId)
        }
    }

    fun isShown(sessionId: SourceMigrationSessionId): Boolean = synchronized(shownCounts) { sessionId in shownCounts }
}
