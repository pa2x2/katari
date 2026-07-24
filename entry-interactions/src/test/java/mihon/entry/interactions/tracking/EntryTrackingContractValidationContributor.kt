package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.host.tracking.EntryTrackingAccountHost
import mihon.entry.interactions.host.tracking.EntryTrackingAutomationHost
import mihon.entry.interactions.host.tracking.EntryTrackingBackupHost
import mihon.entry.interactions.host.tracking.EntryTrackingCollectionHost
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindingOutcome
import mihon.entry.interactions.host.tracking.EntryTrackingHostCollectionSnapshot
import mihon.entry.interactions.host.tracking.EntryTrackingHostCollectionTrack
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntryService
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.host.tracking.EntryTrackingHostService
import mihon.entry.interactions.host.tracking.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.host.tracking.EntryTrackingOperationHost
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractExecutionInput
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractScenario
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

class EntryTrackingContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryTrackingFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.addEntryBackupParticipationContract(
            ENTRY_TRACKING_BACKUP_SNAPSHOT_PARTICIPANT,
            EntryTrackingBehaviorContract.REGISTRY,
            EntryTrackingBackupState.serializer(),
            EntryTrackingBackupState(emptyList()),
        )
        sink.addEntryBackupParticipationContract(
            ENTRY_TRACKING_BACKUP_RESTORE_PARTICIPANT,
            EntryTrackingBehaviorContract.REGISTRY,
            EntryTrackingBackupState.serializer(),
            EntryTrackingBackupState(emptyList()),
        )
        EntryTrackingIntegration.entries.forEach { integration ->
            val contract = EntryTrackingBehaviorContract.valueOf(integration.name)
            val reference = FeatureContractReference(ENTRY_TRACKING_FEATURE_ID, contract)
            sink.add(FeatureContractVerifier(reference) { input -> verifyTracking(input, integration) })
            trackingScenario(integration)?.let { evidence ->
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("${integration.id.value}.applicable"),
                        reference,
                        integration.id,
                    ) { evidence },
                )
            }
        }
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_TRACKING_LIBRARY_ADDITION_PARTICIPANT.id,
                    EntryTrackingLibraryAdditionBehaviorContract,
                ),
                verification = ::verifyLibraryAddition,
            ),
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_TRACKING_PROFILE_MOVE_PARTICIPANT.id,
                    EntryTrackingProfileMoveBehaviorContract,
                ),
            ) {
                verifyFeatureContract {
                    var moved: EntryProfileMoveStateRequest? = null
                    val host = EntryProfileMoveTrackingStateHost { request -> moved = request }
                    val request = EntryProfileMoveStateRequest(1L, 2L, listOf(41L))
                    host.move(request)
                    contractExpectation(moved == request, "Profile movement must transfer Tracking state")
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_TRACKING_MIGRATION_PARTICIPANT.id,
                    EntryTrackingMigrationParticipationBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val source = Entry.create().copy(id = 43L, type = type)
                    val target = source.copy(id = 44L)
                    val preparedTrack = track(target.id).toTrackingRecord()
                    val feature = mockk<EntryTrackingFeature> {
                        coEvery { prepareMigrationTracks(source, target, any()) } returns
                            EntryTrackingMigrationPreparationResult.Prepared(listOf(preparedTrack))
                    }
                    val tracks = mutableListOf<EntryTrack>()
                    entryTrackingMigrationBinding { feature }.handler.execute(
                        EntryMigrationTransitionPreparingEvent(source, target, emptyList(), tracks::addAll),
                    )
                    contractExpectation(
                        tracks == listOf(preparedTrack.toDomainTrack()),
                        "Tracking must contribute prepared records to the Migration transition",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_TRACKING_MERGE_PARTICIPANT.id,
                    EntryTrackingMergeDurableBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val persisted = Entry.create().copy(
                        id = 42L,
                        profileId = 7L,
                        type = type,
                        source = 11L,
                        url = "/merge-tracking",
                    )
                    val feature = mockk<EntryTrackingFeature>(relaxed = true)
                    val binding = entryTrackingMergeBinding(
                        resolveEntry = { _, _, _, _ -> persisted },
                        feature = { feature },
                    )
                    val prepared = binding.preparer.prepare(
                        EntryMergeDurableEvent(
                            operationId = "contract",
                            entry = persisted.copy(id = -1L),
                            changes = setOf(EntryMergeDurableChange.ADDED_TO_LIBRARY),
                            downloadRemovalRequested = false,
                        ),
                    )
                    contractExpectation(prepared != null, "Tracking must prepare Merge library initialization")
                    binding.deliveryHandler.deliver(requireNotNull(prepared))
                    coVerify(exactly = 1) { feature.bindAutomatically(persisted) }
                }
            },
        )
        val mediaSessionReference = FeatureExecutionContractReference(
            ENTRY_TRACKING_MEDIA_SESSION_PARTICIPANT.id,
            EntryTrackingMediaSessionBehaviorContract,
        )
        sink.add(
            FeatureExecutionContractVerifier(mediaSessionReference) { input ->
                verifyFeatureContract {
                    val event = mediaSessionContractEvent(
                        input.provider(EntryProgressCapability.definition).type,
                    )
                    val feature = mockk<EntryTrackingFeature> {
                        coEvery {
                            synchronizeProgress(event.visibleEntry, event.child.chapterNumber, any())
                        } returns EntryTrackingProgressSynchronizationResult.Completed(emptyList())
                    }
                    val execution = EntryMediaSessionExecutionEvent(event).apply {
                        progressResult = EntryProgressRecordingResult(event.progress, completedNow = true)
                    }

                    entryTrackingMediaSessionBinding(
                        feature = { feature },
                        automaticSynchronizationEnabled = { true },
                    ).handler.execute(execution)

                    coVerify(exactly = 1) {
                        feature.synchronizeProgress(event.visibleEntry, event.child.chapterNumber, any())
                    }
                }
            },
        )
        sink.add(
            FeatureExecutionContractScenario(
                FeatureContractScenarioId("entry.tracking.media-session.execution.applicable"),
                mediaSessionReference,
            ) { listOf(contextEvidence(ENTRY_MEDIA_SESSION_TRACKING_ALLOWED, true)) },
        )
    }

    private suspend fun verifyLibraryAddition(
        input: FeatureExecutionContractExecutionInput,
    ) = verifyFeatureContract {
        val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
        val entry = Entry.create().copy(id = 41L, type = type)
        val service = service(type)
        val feature = DefaultEntryTrackingFeature(
            productionSubjectEvaluation(type, EntryTrackingFeatureContributor),
            host(
                service,
                EntryTrackingHostEntrySnapshot(
                    listOf(EntryTrackingHostEntryService(service, true, true, null, null)),
                ),
                track(entry.id),
            ),
        )

        contractExpectation(
            feature.bindAutomatically(entry) is EntryTrackingAutomaticBindingResult.Completed,
            "Library addition must invoke Tracking automatic binding after membership is committed",
        )
    }

    private suspend fun verifyTracking(
        input: FeatureContractExecutionInput,
        integration: EntryTrackingIntegration,
    ) = verifyFeatureContract {
        val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
        val entry = Entry.create().copy(id = 11L, type = type)
        val target = entry.copy(id = 12L)
        val track = track(entry.id)
        val service = service(type)
        val snapshot = EntryTrackingHostEntrySnapshot(
            listOf(EntryTrackingHostEntryService(service, true, true, track, "7.5")),
        )
        val feature = DefaultEntryTrackingFeature(
            productionSubjectEvaluation(type, EntryTrackingFeatureContributor),
            host(service, snapshot, track),
        )

        when (integration) {
            EntryTrackingIntegration.REGISTRY,
            EntryTrackingIntegration.AVAILABILITY,
            -> contractExpectation(
                feature.availability(type) is EntryTrackingAvailability.Available,
                "Tracking must project registered external services",
            )
            EntryTrackingIntegration.SESSION -> contractExpectation(
                feature.observeSession(entry).first() is EntryTrackingSession.Available,
                "Tracking must expose an authenticated accepted session",
            )
            EntryTrackingIntegration.AUTOMATIC_BINDING -> contractExpectation(
                feature.bindAutomatically(entry) is EntryTrackingAutomaticBindingResult.Completed,
                "Tracking must coordinate automatic binding",
            )
            EntryTrackingIntegration.SYNCHRONIZATION -> contractExpectation(
                feature.synchronizeProgress(entry, 5.0) is EntryTrackingProgressSynchronizationResult.Completed,
                "Tracking must coordinate progress synchronization",
            )
            EntryTrackingIntegration.MIGRATION_PREPARATION -> contractExpectation(
                feature.prepareMigrationTracks(entry, target, listOf(track.toTrackingRecord()))
                    is EntryTrackingMigrationPreparationResult.Prepared,
                "Tracking must prepare migration records",
            )
            EntryTrackingIntegration.LIBRARY,
            EntryTrackingIntegration.STATS,
            -> contractExpectation(
                feature.summarizeCollection(setOf(entry.id)) == EntryTrackingCollectionSummary(1, 7.5, 1),
                "Tracking must project collection evidence",
            )
        }
    }

    private fun host(
        service: EntryTrackingHostService,
        snapshot: EntryTrackingHostEntrySnapshot,
        track: EntryTrack,
    ): EntryTrackingHost {
        val automation = mockk<EntryTrackingAutomationHost> {
            coEvery { bindAutomatically(any(), any()) } returns
                listOf(EntryTrackingHostBindingOutcome.NoMatch(service.id, service.name))
            coEvery { synchronizeProgress(any(), any(), any(), any()) } returns emptyList()
            coEvery { reconcileRemoteProgress(any(), any(), any()) } returns Unit
            coEvery { prepareMigrationTracks(any(), any(), any()) } returns listOf(track)
        }
        return object : EntryTrackingHost {
            override val operations: EntryTrackingOperationHost = mockk(relaxed = true)
            override val automation = automation
            override val accounts: EntryTrackingAccountHost = mockk(relaxed = true)
            override val collection = object : EntryTrackingCollectionHost {
                override fun observeCollection() = flowOf(
                    EntryTrackingHostCollectionSnapshot(
                        services = listOf(service),
                        entries = mapOf(
                            track.entryId to listOf(EntryTrackingHostCollectionTrack(service.id, 7.5, true)),
                        ),
                    ),
                )
            }
            override val backup: EntryTrackingBackupHost = EntryTrackingBackupHost.Empty

            override fun registeredServices() = listOf(service)
            override fun observeEntry(entry: Entry) = flowOf(snapshot)
        }
    }

    private fun service(type: EntryType) = EntryTrackingHostService(
        id = 7L,
        name = "Contract Tracker",
        logoResource = 0,
        supportedEntryTypes = setOf(type),
        capabilities = EntryTrackingHostServiceCapabilities(
            statuses = emptyList(),
            scores = emptyList(),
            supportsReadingDates = false,
            supportsPrivateTracking = false,
            supportsRemoteDeletion = false,
            supportsAutomaticBinding = true,
        ),
    )

    private fun track(entryId: Long) = EntryTrack(
        id = 1L,
        entryId = entryId,
        trackerId = 7L,
        remoteId = 2L,
        libraryId = null,
        title = "Tracked",
        progress = 3.0,
        total = 10L,
        status = 1L,
        score = 7.5,
        remoteUrl = "",
        startDate = 0L,
        finishDate = 0L,
        private = false,
    )
}

private fun trackingScenario(
    integration: EntryTrackingIntegration,
): List<mihon.feature.graph.ContextEvidence<*>>? = when (integration) {
    EntryTrackingIntegration.REGISTRY,
    EntryTrackingIntegration.MIGRATION_PREPARATION,
    -> null
    EntryTrackingIntegration.AVAILABILITY -> listOf(contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, true))
    EntryTrackingIntegration.SESSION -> listOf(
        contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, true),
        contextEvidence(ENTRY_TRACKING_AUTHENTICATED_SUPPORT, true),
        contextEvidence(ENTRY_TRACKING_SOURCE_ACCEPTED, true),
    )
    EntryTrackingIntegration.AUTOMATIC_BINDING -> listOf(
        contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, true),
        contextEvidence(ENTRY_TRACKING_AUTHENTICATED_SUPPORT, true),
        contextEvidence(ENTRY_TRACKING_AUTOMATIC_SOURCE_ACCEPTED, true),
    )
    EntryTrackingIntegration.SYNCHRONIZATION -> listOf(
        contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, true),
        contextEvidence(ENTRY_TRACKING_AUTHENTICATED_SUPPORT, true),
        contextEvidence(ENTRY_TRACKING_AUTHENTICATED_TRACK, true),
    )
    EntryTrackingIntegration.LIBRARY,
    EntryTrackingIntegration.STATS,
    -> listOf(
        contextEvidence(ENTRY_TRACKING_REGISTERED_SUPPORT, true),
        contextEvidence(ENTRY_TRACKING_AUTHENTICATED_SUPPORT, true),
    )
}
