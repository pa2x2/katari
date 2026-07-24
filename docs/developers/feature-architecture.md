# Entry Feature architecture

Katari models Entry behavior as a discovered relationship between content-type providers and application Features.
The purpose is to make support and its consequences follow from executable contributions instead of from a checklist
that every developer must remember.

## Layers and ownership

The architecture has four boundaries:

1. A type module contributes the providers it genuinely implements. Provider presence means support; provider absence
   is valid and means unsupported in the current version.
2. A Feature contribution declares prerequisites, common behavior, specialized requirements, contracts, projections,
   execution points, and participants. This is where relationships between capabilities are owned.
3. A Feature runtime module installs that contribution together with its application-facing Feature implementation,
   executable participant bindings, and warmups. Production installs the module once and derives those artifacts from
   it.
4. Screens, workers, notifications, and other app consumers call the application-facing `Entry…Feature`. They do not
   dispatch type providers, inspect the Feature Graph, or recreate availability rules.

In simplified form:

```kotlin
// entry-interactions:audio
class AudioPlugin : EntryInteractionPlugin {
    override val type = EntryType.AUDIO
    override val providerBindings = listOf(
        EntryOpenCapability.bind(AudioOpenProvider()),
        // No Download provider yet, so Download is simply unavailable for Audio.
    )
}

// entry-interactions root
val ExampleFeatureModule = EntryFeatureRuntimeModule(
    id = "entry.example",
    contributor = ExampleFeatureContribution,
    installRuntime = {
        addSingletonFactory<EntryExampleFeature> {
            DefaultEntryExampleFeature(/* graph-selected providers and host dependencies */)
        }
        EntryFeatureRuntimeArtifacts(
            runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryExampleFeature>() }),
        )
    },
)

// app
val result = entryExampleFeature.execute(request)
```

Adding Download support later changes the Audio type contribution. Existing Download integrations, policies,
contracts, reporting, and generic UI availability are then selected from that provider automatically. Extra work is
required only when Download declares a genuine Audio-specific adapter or another specialized obligation; validation
names that missing artifact.

## Adding a content type

Create one type interaction module and contribute only the providers currently implemented. Do not add false providers,
intentional-absence markers, or a matrix saying which operations the type lacks. A type with only Open support is a
valid contribution.

Type-specific mechanics stay behind the provider SPI. Generic app code must not import the type module or branch on
the new `EntryType` to execute behavior. If adding the type reveals such a branch, move that relationship into the
Feature that owns it or contribute a specialized adapter when the mechanics cannot be shared.

## Adding a Feature

A new Feature owns one cohesive contribution and runtime module:

```kotlin
object EntryExampleFeatureContribution : FeatureGraphContributor {
    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            exampleIntegration(
                required = EntryExampleCapability,
                behavioralContracts = listOf(exampleBehaviorContract),
            ),
        )
    }
}

internal val EntryExampleFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.example",
    contributor = EntryExampleFeatureContribution,
    installRuntime = { context ->
        val feature = DefaultEntryExampleFeature(/* graph-selected providers and host dependencies */)
        addSingletonFactory<EntryExampleFeature> { feature }
        EntryFeatureRuntimeArtifacts(
            runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { feature }),
        )
    },
)
```

Declare the module next to its owner in an `*.entry-feature-module` file:

```properties
id=entry.example
module=mihon.entry.interactions.EntryExampleFeatureRuntimeModule
```

The descriptor is the production registration. Its ID must match the runtime module ID, and its symbol must resolve to
an `EntryFeatureRuntimeModule`. The active Android variant discovers descriptors from `src/main` and its own source set,
then generates the Kotlin production registry in stable ID order.

Declare shared behavior from provider prerequisites rather than naming current content types. If the Feature requires a
media-specific implementation, declare a specialized requirement so a supporting type becomes incomplete visibly
instead of silently losing the behavior.

Behavioral contracts belong to the Feature and run for every graph-selected subject. Their validation contributor is
loaded through the `FeatureValidationContributor` service registry; the build rejects a declaration omitted from that
registry, duplicate entries, and unknown registrations. Tests should exercise observable behavior, not repeat which
types have providers.

Content-type modules use the same owner-local mechanism. Place an `*.entry-type-module` descriptor beside the type
factory:

```properties
id=example
factory=mihon.entry.interactions.example.exampleEntryTypeRuntimeModule
```

There is no hand-written production topology list. The generator discovers both descriptor kinds, rejects malformed or
duplicate registrations, and emits direct Kotlin references. Compilation therefore rejects missing or wrongly typed
symbols without reflection, `ServiceLoader`, or Kotlin source-text parsing. Runtime installation and architecture
validation consume that same generated composition and validate declared IDs and contributor ownership.

## Contributing cross-Feature consequences

When one coordinator must invoke independently owned work, it exposes a typed execution point. The affected Feature or
host contributes its own participant and runtime binding:

```kotlin
val libraryAdded = afterCommitVolatileFeatureExecutionPointDefinition<EntryLibraryAddedEvent>(
    failurePolicy = CONTINUE_AND_REPORT,
)

val trackingParticipant = FeatureExecutionParticipantDefinition(
    point = libraryAdded,
    prerequisites = TrackingCapability,
    behavioralContracts = listOf(trackingAfterLibraryAddContract),
)

FeatureExecutionParticipantBinding(
    definition = trackingParticipant,
    handler = FeatureExecutionHandler { event ->
        trackingFeature.bindAutomatically(event.entry)
    },
)
```

The coordinator executes applicable participants selected by the evaluated graph; it does not switch on participant or
Feature IDs. Lifecycle phase and failure policy belong to the execution point. Point types are phase-specific:

- `inlineFeatureExecutionPointDefinition` runs synchronously at the caller's current lifecycle position and makes no
  persistence guarantee;
- `transactionalFeatureExecutionPointDefinition` can execute only through a restricted scope inside a host callback;
- `afterCommitVolatileFeatureExecutionPointDefinition` is process-local and can execute only after a successful commit
  result;
- `durableFeatureExecutionPointDefinition` prepares persistable work for eventual delivery.

For atomic workflows, `coordinateFeatureCommit` gives the coordinator a callback factory rather than a transaction
execution scope. The coordinator passes those callbacks to its persistence host, and the host invokes them inside the
actual database transaction. The runtime releases volatile post-commit work only when the host result is classified as
committed:

```kotlin
executions.coordinateFeatureCommit(
    commit = {
        host.remove(
            entries,
            beforeDelete = callback { persisted ->
                execute(removingPoint, type, RemovingEvent(persisted)).requireSuccessful()
            },
        )
    },
    committed = { it is RemovalCommit.Applied },
    volatileConsequences = { commit ->
        execute(removedPoint, type, RemovedEvent(commit.entries))
    },
)
```

The host owns the database boundary; the coordinator cannot manufacture either restricted execution scope. Ordering is
declared only for real dependencies. Composition rejects missing, orphaned, duplicate, unknown, unreachable, or
uncontracted executable participants.

Descriptive projections are different: they report or render already-derived truth and have no execution path. Do not
use a projection ID as a runtime dispatch key.

### Media-session consequences

Readers, players, and immersive viewers report structured `EntryMediaSessionEvent` facts through the Media Session
Feature. They do not persist progress or history, synchronize trackers, invoke Download policy, or decide whether
incognito suppresses those recording consequences themselves. Reader and player UI may still observe incognito for
interface lifecycle or secure-screen behavior.

Progress, History, Tracking, Download Lifecycle, and incognito policy contribute independently to the Media Session
execution points. Incognito policy runs before consequences and suppresses recording behavior without becoming a
type-specific reader branch. Progress publishes the authoritative completion transition used by later Tracking and
Download participants. Participants declare their real ordering dependency; the Media Session coordinator does not
name them.

A type that implements a media runtime contributes an operational `EntryMediaSessionProcessor` and uses that same
processor to emit its events. That declaration automatically selects the shared consequences and their contracts.
Adding another consequence changes only its owning Feature contribution and runtime binding. Adding another media type
does not require edits to the coordinator or to existing consequence owners unless a genuinely specialized contract is
reported as incomplete.

### Refresh consequences

Manual Entry refresh and Library update emit separate typed new-child events because their lifecycle guarantees differ.
Source Refresh emits its event after a successful manual refresh. Library Update creates one refresh session, emits an
event for every successfully refreshed Entry, and completes the session once after its concurrent refresh work. Every
Source Refresh caller must state whether its request is manual; there is no default that can silently omit the
manual-refresh consequences.

Automatic Download contributes to both points. Its manual-refresh participant applies policy immediately. Its
Library-update participant owns a deferred batch in the generic refresh session, queues accepted children while source
work is active, and starts Download processing once when the session completes. The Library worker does not know which
participant owns that state or what completion means; it retains ownership only of scheduling, progress and result
notifications, and failure reporting.

```kotlin
val session = entryLibraryUpdateRefreshFeature.newSession()
entries.concurrentForEach { entry ->
    session.refresh(EntryLibraryUpdateRefreshRequest(entry))
}
session.complete()

val futureParticipant = FeatureExecutionParticipantDefinition(
    point = libraryUpdateNewChildren,
    prerequisites = FutureCapability,
    behavioralContracts = listOf(futureRefreshContract),
)
```

Adding another refresh consequence therefore changes only its owning contribution and runtime binding. It does not add
a call to the Entry screen, Library worker, Source Refresh coordinator, or Library Refresh coordinator. Participant
absence remains valid when its prerequisites are absent.

### Consumer lifecycle boundaries

Application and type-runtime consumers must enter shared behavior through the Feature that owns it, even when the
underlying persistence call looks simple. A media runtime that changes a child bookmark calls `EntryBookmarkFeature`;
it does not update the child repository directly. This preserves provider dispatch, contextual policy, behavioral
contracts, and future Bookmark consequences for that consumer.

Permanent profile deletion first sends every Entry owned by the profile through `EntryDestructiveRemovalFeature`.
Download cleanup, Merge cleanup, custom covers, and future Entry-removal participants are therefore discovered from
their owners before the profile row is removed. Remaining persisted state follows declared database ownership through
foreign-key cascades, while the profile host removes its own preference namespace. Profile code must not maintain a
parallel list of Feature tables to clear.

No separate profile-wide Feature participant is currently required because the audited state is Entry-owned,
relationally profile-owned, or core profile-host state. If a Feature later introduces independent profile-wide state,
it must introduce a discoverable profile lifecycle boundary rather than append another delete statement to profile
orchestration.

### Durable consequences

Work that must survive process death uses the same discovered participant model with a durable execution point. A
durable participant owns three operations:

- preparation of an opaque, versioned payload from the typed workflow event;
- delivery of that payload after the workflow host has committed it;
- optional discard of prepared state when the workflow cannot commit.

```kotlin
val migrated = durableFeatureExecutionPointDefinition<EntryMigratedEvent>(
    failurePolicy = FAIL_FAST,
)

val exampleParticipant = FeatureExecutionParticipantDefinition(
    point = migrated,
    prerequisites = ExampleCapability,
    behavioralContracts = listOf(exampleMigrationContract),
)

FeatureDurableExecutionParticipantBinding(
    definition = exampleParticipant,
    preparer = FeatureDurableExecutionPreparer { event ->
        exampleFeature.prepare(event)?.let { state ->
            FeatureDurableExecutionPayload(schemaVersion = 1, value = codec.encode(state))
        }
    },
    deliveryHandler = FeatureDurableExecutionDeliveryHandler { payload ->
        exampleFeature.apply(codec.decode(payload.schemaVersion, payload.value))
    },
)
```

The workflow coordinator asks the execution runtime to prepare all applicable participants. Its persistence host stores
each returned envelope as participant ID, schema version, and opaque payload. Delivery routes through the installed
participant binding; the coordinator never switches on participant IDs and never interprets payload schemas.

Unknown persisted participant IDs are an explicit delivery failure and remain pending for retry or compatibility
handling. They must not be acknowledged or silently discarded. If persistence of a prepared plan fails, the
coordinator asks the runtime to discard the prepared envelopes so participant-owned staging can be cleaned up.

An ordinary `FeatureExecutionParticipantBinding` cannot bind a durable point, and a durable binding cannot bind an
transient point. Production composition rejects missing, duplicate, orphaned, contradictory, and uncontracted durable
participants in the same way as transient participants.

Entry Migration and Merge use this boundary for optional cross-Feature work. Their hosts persist only generic workflow
identity, participant ID, schema version, and opaque payload, while each owner contributes its own preparation and
delivery. Merge emits entry-level workflow facts; Tracking, Download maintenance, custom-cover ownership, and any
future owner interpret only the facts relevant to them. Migration option discovery and transition preparation use
inline execution points for the same reason: adding another participating Feature changes that Feature's contribution
and runtime binding, not a coordinator-owned completion list or dispatch switch. Backup restore is also inline: its
current repository sequence is a logical restore operation, not one shared database transaction, so it must not claim
the stronger transactional contract.

Finite adapters may remain for payloads persisted before owner-contributed delivery existed. They must be explicitly
schema-bounded and must not select or dispatch current participants; current payload schemas always use the discovered
runtime.

## Contributing Entry backup state

Feature-owned Entry state participates in the Backup Feature's snapshot and restore execution points. The participant
owns a stable ID, payload codec, and schema version and writes an opaque `EntryFeatureStateEnvelope`:

```kotlin
snapshot.add(
    EntryFeatureStateEnvelope(
        participantId = EXAMPLE_BACKUP_PARTICIPANT_ID,
        schemaVersion = 1,
        payload = exampleCodec.encode(state),
    ),
)
```

Generic backup orchestration does not know the Feature's fields or codec. Restore prefers the envelope owned by the
participant. Unknown future envelopes do not invalidate the rest of a backup. Existing typed fields may remain in a
finite compatibility adapter while older backups or older Katari releases require them, but they must never select
current participants.

Use a finalization participant only when restoration genuinely depends on other Entries already existing. Mutable
restore accumulation belongs to the caller-owned restore session, not a production singleton.

## App-owned projections

Some behavior requires a real app implementation that cannot live in a type module, such as a Compose Viewer Settings
screen. Such projections are specialized obligations, not separate support flags. Production composition registers the
real implementations, runtime validation matches them exactly to provider-owned surfaces, and build validation rejects
an unregistered, duplicated, or unknown projection.

Generic navigation, labels, and side effects should instead project directly from the Feature result. They do not need
another contribution when the Feature already returns all required data.

Likewise, an operation may own current-context availability and return a structured unavailable result. Presentation
may keep an action visible and handle that result without declaring separate content-type support. Add a preflight
projection only when another consumer genuinely needs availability before invocation, not merely to duplicate volatile
operation context.

## Verification and reporting

Run the complete architecture gate after changing a type, Feature, participant, contract, or projection:

```bash
./gradlew verifyEntryFeatureArchitecture
```

The gate generates the active production registry, checks boundaries and installation, runs graph and behavioral
validation, verifies generated documentation, and generates the developer report. The content-type reference is a
projection of the same evaluated graph; do not edit generated capability truth by hand.

Before adding an allowlist or exception, ask whether an unknown future type or Feature could now be omitted without the
architecture noticing. If so, extend discovery or validation rather than documenting a new checklist.
