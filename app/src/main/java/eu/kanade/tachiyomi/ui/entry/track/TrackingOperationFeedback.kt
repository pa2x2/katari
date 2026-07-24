package eu.kanade.tachiyomi.ui.entry.track

import logcat.LogPriority
import mihon.entry.interactions.EntryTrackingOperationResult
import mihon.entry.interactions.EntryTrackingRemovalResult
import tachiyomi.core.common.util.system.logcat

internal fun EntryTrackingOperationResult.logFailure(operation: String) {
    when (this) {
        EntryTrackingOperationResult.Completed -> Unit
        is EntryTrackingOperationResult.Failed -> logcat(LogPriority.ERROR, cause) {
            "Tracking $operation failed"
        }
        is EntryTrackingOperationResult.Unavailable -> logcat(LogPriority.WARN) {
            "Tracking $operation became unavailable: $reason"
        }
    }
}

internal fun EntryTrackingRemovalResult.logFailure() {
    when (this) {
        is EntryTrackingRemovalResult.Removed -> remoteDeletionFailure?.let { failure ->
            logcat(LogPriority.ERROR, failure) { "Failed to delete entry from tracking service" }
        }
        is EntryTrackingRemovalResult.Failed -> logcat(LogPriority.ERROR, cause) {
            "Failed to remove local tracking record"
        }
        is EntryTrackingRemovalResult.Unavailable -> logcat(LogPriority.WARN) {
            "Tracking removal became unavailable: $reason"
        }
    }
}
