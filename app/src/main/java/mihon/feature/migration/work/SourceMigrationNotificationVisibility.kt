package mihon.feature.migration.work

import mihon.feature.migration.session.model.SourceMigrationSessionId

internal object SourceMigrationNotificationVisibility {
    private val visibleSessionCounts = mutableMapOf<SourceMigrationSessionId, Int>()

    @Synchronized
    fun show(sessionId: SourceMigrationSessionId) {
        visibleSessionCounts[sessionId] = visibleSessionCounts.getOrDefault(sessionId, 0) + 1
    }

    @Synchronized
    fun hide(sessionId: SourceMigrationSessionId) {
        val remaining = visibleSessionCounts.getOrDefault(sessionId, 0) - 1
        if (remaining > 0) {
            visibleSessionCounts[sessionId] = remaining
        } else {
            visibleSessionCounts.remove(sessionId)
        }
    }

    @Synchronized
    fun isVisible(sessionId: SourceMigrationSessionId): Boolean {
        return visibleSessionCounts.containsKey(sessionId)
    }
}
