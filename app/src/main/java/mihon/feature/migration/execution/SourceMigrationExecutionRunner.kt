package mihon.feature.migration.execution

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.migration.EntryMigrationExecuteIntent
import mihon.entry.interactions.migration.EntryMigrationExecutionResult
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationFollowUp
import mihon.entry.interactions.migration.EntryMigrationMode
import mihon.entry.interactions.migration.EntryMigrationOperationIntent
import mihon.entry.interactions.migration.EntryMigrationOperationReconciliationResult
import mihon.entry.interactions.migration.EntryMigrationPreparationResult
import mihon.entry.interactions.migration.EntryMigrationPrepareIntent
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionItem
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import tachiyomi.domain.entry.repository.EntryRepository

class SourceMigrationExecutionRunner(
    private val store: SourceMigrationSessionStore,
    private val entryRepository: EntryRepository,
    private val migration: EntryMigrationFeature,
) {
    suspend fun run(
        sessionId: SourceMigrationSessionId,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): SourceMigrationExecutionRunResult {
        val session = startOrReload(sessionId) ?: return SourceMigrationExecutionRunResult.SessionMissing
        if (session.stage == SourceMigrationSessionStage.EXECUTION_PAUSED || session.cancellationRequested) {
            pause(sessionId, session.stage)
            return SourceMigrationExecutionRunResult.Paused
        }
        if (session.stage != SourceMigrationSessionStage.EXECUTING) {
            return SourceMigrationExecutionRunResult.NoWork
        }

        val items = session.items.filter { it.state in RUNNABLE_ITEM_STATES }
        var completed = session.items.count { it.included && it.state in TERMINAL_EXECUTION_STATES }
        val total = items.size + completed
        onProgress(completed, total)
        for (item in items) {
            val current = store.get(sessionId) ?: return SourceMigrationExecutionRunResult.SessionMissing
            if (current.stage != SourceMigrationSessionStage.EXECUTING || current.cancellationRequested) {
                pause(sessionId, current.stage)
                return SourceMigrationExecutionRunResult.Paused
            }
            executeItem(current, item)
            completed++
            onProgress(completed, total)
        }

        val current = store.get(sessionId) ?: return SourceMigrationExecutionRunResult.SessionMissing
        if (current.stage != SourceMigrationSessionStage.EXECUTING || current.cancellationRequested) {
            pause(sessionId, current.stage)
            return SourceMigrationExecutionRunResult.Paused
        }
        store.transitionStage(
            sessionId = sessionId,
            expected = SourceMigrationSessionStage.EXECUTING,
            new = SourceMigrationSessionStage.COMPLETED,
        )
        val finished = store.get(sessionId) ?: return SourceMigrationExecutionRunResult.SessionMissing
        val executedItems = finished.items.filter { it.included && it.state in TERMINAL_EXECUTION_STATES }
        return SourceMigrationExecutionRunResult.Completed(
            migratedItems = executedItems.count { it.state in APPLIED_STATES },
            attentionItems = executedItems.count { it.state != SourceMigrationItemState.APPLIED },
            totalItems = executedItems.size,
        )
    }

    private suspend fun startOrReload(sessionId: SourceMigrationSessionId): SourceMigrationSession? {
        val session = store.get(sessionId) ?: return null
        if (session.stage == SourceMigrationSessionStage.EXECUTION_QUEUED) {
            store.transitionStage(
                sessionId = sessionId,
                expected = SourceMigrationSessionStage.EXECUTION_QUEUED,
                new = SourceMigrationSessionStage.EXECUTING,
            )
            return store.get(sessionId)
        }
        return session
    }

    private suspend fun pause(sessionId: SourceMigrationSessionId, currentStage: SourceMigrationSessionStage) {
        if (currentStage == SourceMigrationSessionStage.EXECUTING) {
            store.transitionStage(sessionId, currentStage, SourceMigrationSessionStage.EXECUTION_PAUSED)
        } else if (currentStage == SourceMigrationSessionStage.EXECUTION_QUEUED) {
            store.transitionStage(sessionId, currentStage, SourceMigrationSessionStage.EXECUTION_PAUSED)
        }
    }

    private suspend fun executeItem(
        session: SourceMigrationSession,
        item: SourceMigrationSessionItem,
    ) {
        val targetEntryId = item.selectedTargetEntryId
        if (targetEntryId == null) {
            recordFailure(session, item, SourceMigrationItemState.CONFLICT, ERROR_TARGET_MISSING)
            return
        }
        store.recordExecutionState(
            session.id,
            item.sourceEntryId,
            SourceMigrationItemState.EXECUTING,
            incrementAttempt = true,
        )
        val options = session.selectedOptions.intersect(item.availableOptions)
        val operation = EntryMigrationOperationIntent(
            key = item.operationKey,
            source = EntryMigrationSubject(session.profileId, item.sourceEntryId),
            target = EntryMigrationSubject(session.profileId, targetEntryId),
            mode = EntryMigrationMode.REPLACE,
            selectedOptions = options,
        )

        try {
            when (val reconciliation = migration.reconcileOperation(operation)) {
                is EntryMigrationOperationReconciliationResult.Applied -> {
                    recordApplied(session, item, reconciliation.outcome.followUp)
                    return
                }
                EntryMigrationOperationReconciliationResult.Conflict -> {
                    recordFailure(session, item, SourceMigrationItemState.CONFLICT, ERROR_OPERATION_CONFLICT)
                    return
                }
                is EntryMigrationOperationReconciliationResult.OperationalFailure -> {
                    recordFailure(
                        session,
                        item,
                        if (reconciliation.retryable) {
                            SourceMigrationItemState.EXECUTION_FAILED
                        } else {
                            SourceMigrationItemState.CONFLICT
                        },
                        ERROR_OPERATION_RECONCILIATION,
                    )
                    return
                }
                EntryMigrationOperationReconciliationResult.NotApplied -> Unit
            }

            val source = entryRepository.getEntryById(item.sourceEntryId, session.profileId)
            val target = entryRepository.getEntryById(targetEntryId, session.profileId)
            if (source == null || target == null) {
                recordFailure(session, item, SourceMigrationItemState.CONFLICT, ERROR_ENTRY_MISSING)
                return
            }
            when (
                val preparation = migration.prepare(
                    EntryMigrationPrepareIntent(source, target, item.operationKey),
                )
            ) {
                is EntryMigrationPreparationResult.Ready -> {
                    if (!preparation.availableOptions.containsAll(options)) {
                        recordFailure(session, item, SourceMigrationItemState.CONFLICT, ERROR_OPTIONS_CHANGED)
                        return
                    }
                    when (
                        val result = migration.execute(
                            EntryMigrationExecuteIntent(
                                reference = preparation.reference,
                                mode = EntryMigrationMode.REPLACE,
                                selectedOptions = options,
                            ),
                        )
                    ) {
                        is EntryMigrationExecutionResult.Applied -> {
                            recordApplied(session, item, result.outcome.followUp)
                        }
                        EntryMigrationExecutionResult.Conflict -> {
                            recordFailure(session, item, SourceMigrationItemState.CONFLICT, ERROR_PAIR_CONFLICT)
                        }
                        is EntryMigrationExecutionResult.Rejected -> {
                            recordFailure(session, item, SourceMigrationItemState.CONFLICT, result.reason.name)
                        }
                        is EntryMigrationExecutionResult.OperationalFailure -> {
                            recordFailure(
                                session,
                                item,
                                if (result.retryable) {
                                    SourceMigrationItemState.EXECUTION_FAILED
                                } else {
                                    SourceMigrationItemState.CONFLICT
                                },
                                ERROR_PAIR_EXECUTION,
                            )
                        }
                    }
                }
                is EntryMigrationPreparationResult.Rejected -> {
                    recordFailure(session, item, SourceMigrationItemState.CONFLICT, preparation.reason.name)
                }
                is EntryMigrationPreparationResult.OperationalFailure -> {
                    recordFailure(
                        session,
                        item,
                        if (preparation.retryable) {
                            SourceMigrationItemState.EXECUTION_FAILED
                        } else {
                            SourceMigrationItemState.CONFLICT
                        },
                        ERROR_PAIR_PREPARATION,
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            recordFailure(
                session,
                item,
                SourceMigrationItemState.EXECUTION_FAILED,
                ERROR_EXECUTION_OPERATION,
                error.message,
            )
        }
    }

    private suspend fun recordApplied(
        session: SourceMigrationSession,
        item: SourceMigrationSessionItem,
        followUp: EntryMigrationFollowUp,
    ) {
        store.recordExecutionState(
            session.id,
            item.sourceEntryId,
            if (followUp == EntryMigrationFollowUp.COMPLETE) {
                SourceMigrationItemState.APPLIED
            } else {
                SourceMigrationItemState.APPLIED_INCOMPLETE
            },
            incrementAttempt = false,
        )
    }

    private suspend fun recordFailure(
        session: SourceMigrationSession,
        item: SourceMigrationSessionItem,
        state: SourceMigrationItemState,
        errorCode: String,
        errorMessage: String? = null,
    ) {
        store.recordExecutionState(
            session.id,
            item.sourceEntryId,
            state,
            incrementAttempt = false,
            errorCode = errorCode,
            errorMessage = errorMessage,
        )
    }

    private companion object {
        val RUNNABLE_ITEM_STATES = setOf(
            SourceMigrationItemState.EXECUTION_QUEUED,
            SourceMigrationItemState.EXECUTING,
        )
        val APPLIED_STATES = setOf(
            SourceMigrationItemState.APPLIED,
            SourceMigrationItemState.APPLIED_INCOMPLETE,
        )
        val TERMINAL_EXECUTION_STATES = APPLIED_STATES + setOf(
            SourceMigrationItemState.EXECUTION_FAILED,
            SourceMigrationItemState.CONFLICT,
            SourceMigrationItemState.CANCELLED,
        )

        const val ERROR_TARGET_MISSING = "TARGET_MISSING"
        const val ERROR_OPERATION_CONFLICT = "OPERATION_CONFLICT"
        const val ERROR_OPERATION_RECONCILIATION = "OPERATION_RECONCILIATION_FAILED"
        const val ERROR_ENTRY_MISSING = "ENTRY_MISSING"
        const val ERROR_OPTIONS_CHANGED = "AVAILABLE_OPTIONS_CHANGED"
        const val ERROR_PAIR_CONFLICT = "PAIR_CONFLICT"
        const val ERROR_PAIR_EXECUTION = "PAIR_EXECUTION_FAILED"
        const val ERROR_PAIR_PREPARATION = "PAIR_PREPARATION_FAILED"
        const val ERROR_EXECUTION_OPERATION = "EXECUTION_OPERATION_FAILED"
    }
}
