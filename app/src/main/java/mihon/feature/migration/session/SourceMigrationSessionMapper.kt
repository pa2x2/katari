package mihon.feature.migration.session

import mihon.entry.interactions.migration.EntryMigrationOperationKey
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationDiscoveryFailure
import mihon.feature.migration.session.model.SourceMigrationDiscoveryFailureReason
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationMatchKind
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionGroup
import mihon.feature.migration.session.model.SourceMigrationSessionGroupMember
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionItem
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import tachiyomi.data.Source_migration_candidates
import tachiyomi.data.Source_migration_discovery_failures
import tachiyomi.data.Source_migration_group_members
import tachiyomi.data.Source_migration_groups
import tachiyomi.data.Source_migration_items
import tachiyomi.data.Source_migration_sessions

internal fun Source_migration_sessions.toDomain(
    targetSourceIds: List<Long>,
    groups: List<SourceMigrationSessionGroup>,
    items: List<SourceMigrationSessionItem>,
): SourceMigrationSession {
    return SourceMigrationSession(
        id = SourceMigrationSessionId(session_id),
        profileId = profile_id,
        originSourceId = origin_source_id,
        stage = SourceMigrationSessionStage.valueOf(stage),
        targetSourceIds = targetSourceIds,
        selectedOptions = selected_options.decodeOptions(),
        cancellationRequested = cancellation_requested,
        errorCode = error_code,
        errorMessage = error_message,
        createdAt = created_at,
        updatedAt = updated_at,
        completedAt = completed_at,
        groups = groups,
        items = items,
    )
}

internal fun Source_migration_groups.toDomain(
    members: List<SourceMigrationSessionGroupMember>,
): SourceMigrationSessionGroup {
    return SourceMigrationSessionGroup(
        sessionId = SourceMigrationSessionId(session_id),
        groupId = group_id,
        position = position,
        visibleEntryId = visible_entry_id,
        visibleTitle = visible_title,
        members = members,
    )
}

internal fun Source_migration_group_members.toDomain(): SourceMigrationSessionGroupMember {
    return SourceMigrationSessionGroupMember(
        entryId = entry_id,
        position = position,
        sourceId = source_id,
        title = title,
        url = url,
        thumbnailUrl = thumbnail_url,
        selected = selected,
    )
}

internal fun Source_migration_items.toDomain(): SourceMigrationSessionItem {
    return SourceMigrationSessionItem(
        sessionId = SourceMigrationSessionId(session_id),
        sourceEntryId = source_entry_id,
        position = position,
        sourceId = source_id,
        sourceTitle = source_title,
        sourceUrl = source_url,
        sourceThumbnailUrl = source_thumbnail_url,
        state = SourceMigrationItemState.valueOf(state),
        searchDepth = SourceMigrationDiscoveryDepth.valueOf(search_depth),
        included = included,
        selectedTargetEntryId = selected_target_entry_id,
        targetSourceId = target_source_id,
        targetTitle = target_title,
        targetUrl = target_url,
        targetThumbnailUrl = target_thumbnail_url,
        matchKind = match_kind?.let(SourceMigrationMatchKind::valueOf),
        operationKey = EntryMigrationOperationKey(operation_key),
        availableOptions = available_options.decodeOptions(),
        discoveryAttempts = discovery_attempts,
        executionAttempts = execution_attempts,
        errorCode = error_code,
        errorMessage = error_message,
        updatedAt = updated_at,
    )
}

internal fun Source_migration_candidates.toDomain(): SourceMigrationCandidate {
    return SourceMigrationCandidate(
        sessionId = SourceMigrationSessionId(session_id),
        sourceEntryId = source_entry_id,
        targetSourceId = target_source_id,
        targetTitle = target_title,
        targetUrl = target_url,
        targetThumbnailUrl = target_thumbnail_url,
        rank = rank,
        score = score,
        matchKind = SourceMigrationMatchKind.valueOf(match_kind),
        discoveredAt = discovered_at,
    )
}

internal fun Source_migration_discovery_failures.toDomain(): SourceMigrationDiscoveryFailure {
    return SourceMigrationDiscoveryFailure(
        sessionId = SourceMigrationSessionId(session_id),
        sourceEntryId = source_entry_id,
        targetSourceId = target_source_id,
        reason = SourceMigrationDiscoveryFailureReason.valueOf(reason),
        retryable = retryable,
        detail = detail,
        updatedAt = updated_at,
    )
}

internal fun Set<EntryMigrationOption>.encodeOptions(): String {
    return map(EntryMigrationOption::name).sorted().joinToString(",")
}

private fun String.decodeOptions(): Set<EntryMigrationOption> {
    return takeIf(String::isNotEmpty)
        ?.split(',')
        ?.mapTo(mutableSetOf(), EntryMigrationOption::valueOf)
        .orEmpty()
}
