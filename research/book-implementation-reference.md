# BOOK implementation reference

Execution guide for BOOK support. The authoritative architecture and rationale
are in [`book-content-support.md`](book-content-support.md); if this guide and the
research record differ, the research record wins.

## Working rules

- Complete and validate one phase before starting the next.
- Anime and manga progress cutovers are one-shot; do not add dual-read/write
  compatibility paths.
- Preserve legacy backup restore while writing only generic progress.
- Keep source/site logic, processor/format logic, and Katari lifecycle/storage
  responsibilities on their documented sides of the boundary.
- Keep Readium private to `:entry-interactions:book`.
- Device/AVD validation requires separate authorization and a prepared target.

## Milestone status

| Phase | Status |
| --- | --- |
| 1 — shared progress foundation | Complete (`5edd3e12a`) |
| 2 — anime one-shot cutover | Complete (`0de598118`) |
| 3 — manga one-shot cutover | Complete (`fa552ecf6`) |
| 4 — BOOK SDK/runtime | Complete (`6c62bef61`) |
| 5 — EPUB production integration | In progress (built-in processor, reader runtime, and release assembly milestone pending review) |

## Phase 1 — shared progress foundation

Reference: [Shared progress architecture](book-content-support.md#shared-progress-architecture).

Deliverables:

1. Add `entry_progress_state` with stable keys, optional child mapping, common
   locator columns, extension JSON, independent clocks, sparse tombstones, FKs,
   indexes, and SQL constraints.
2. Add domain model, mapper, repository, field-wise conflict helpers, sanitation,
   and transactional child-read projection.
3. Add `EntryProgressInteraction` snapshot/restore/copy registry and default
   behavior for unsupported entry types.
4. Separate anime playback preferences from progress snapshots.
5. Add generic backup models; reserve/read legacy manga and anime fields.

Gate:

- schema compiles and clean databases create correctly;
- repository, conflict, reset, unknown-extension, and constraint tests pass;
- generic interaction-boundary tests pass;
- no manga/anime runtime behavior changes yet.

## Phase 2 — anime one-shot cutover

Reference: [Migration rules](book-content-support.md#migration-rules).

Deliverables:

1. Backfill the union of meaningful `playback_state` and completed anime child
   state, sanitizing invalid values and using only trustworthy clocks.
2. Switch player persistence/restoration, Continue, child labels, library and
   updates progress, immersive feed, backup/restore, and merge-copy.
3. Keep history independent and preserve player preferences.
4. Remove `playback_state` and obsolete playback-progress APIs in the same
   cutover.

Gate:

- seeded historical upgrade preserves position, duration, completion, and read
  projection;
- player resume, backward seek, completion/uncompletion, filters, Continue,
  history duration, backup/restore, and merge-copy tests pass;
- anime compile and focused unit suites pass.

## Phase 3 — manga one-shot cutover

Reference: [Migration rules](book-content-support.md#migration-rules).

Deliverables:

1. Backfill chapters with partial or completed manga state using zero-based page
   locators and trustworthy history clocks.
2. Switch reader/immersive persistence and restoration, child labels, Continue,
   library/updates filters, backup/restore, sync, and merge-copy.
3. Preserve current mark-unread reset behavior and independent history.
4. Rebuild `chapters` without `last_page_read` and remove obsolete APIs/views.

Gate:

- seeded historical upgrade preserves partial pages and completion;
- pager/webtoon resume, completion/uncompletion, filters, Continue, history,
  backup/restore, sync, and merge-copy tests pass;
- manga compile and focused reader/domain suites pass.

## Phase 4 — BOOK SDK and runtime integration

References:

- [Runtime ownership](book-content-support.md#runtime-ownership)
- [Source SDK and entry screen](book-content-support.md#source-sdk-and-entry-screen)
- [Processor-facing data and access](book-content-support.md#processor-facing-data-and-access)

Deliverables:

1. Add `EntryType.BOOK` presentation and interaction registration.
2. Publish/share stable `book-api` models and add explicit
   `EntryMedia.Book` source models, catalogs, hierarchy, availability, and sealed
   data-only locations.
3. Implement the Katari source-to-`BookContentSession` adapter using existing
   `getMedia(chapter)`, resource leases, cancellation, limits, and cache/download
   ownership.
4. Implement processor registry, compatibility filtering, remembered chooser,
   and unsupported-content UI.
5. Implement BOOK open/Continue/progress/history and entry-screen projection on
   the proven shared progress contract.

Gate:

- generic source ABI and serialization tests pass;
- source-child, remote, inline, and local resource contract tests pass;
- lifecycle/cancellation/failure and interaction-boundary tests pass;
- app compiles without launching a processor-specific reader.

## Phase 5 — EPUB production integration

Reference: [Readium EPUB evaluation](book-content-support.md#readium-epub-evaluation).

Deliverables:

1. Wire the existing Readium EPUB processor into the BOOK runtime.
2. Complete the processor-owned reader UI and precise locator reporting/restore.
3. Add production EPUB download/cache handling and hostile archive/unsupported
   feature limits.
4. Validate effective Media3 resolution against anime, R8/minified release,
   final APK size, and dependency/license attribution.
5. With separate authorization, validate rendering, pagination, gestures,
   configuration/process restoration, lifecycle, and resource loading on the
   prepared AVD/device.

Gate:

- passive reflowable EPUB 2/3 scope works end to end;
- unsupported/malformed content reaches dedicated structured UI;
- progress survives reopen, process restoration, and supported revision changes;
- all production adoption conditions in the research record are satisfied.

## Validation baseline

Run focused checks during each phase, then the repository baseline appropriate
to the touched surface:

```text
./gradlew --quiet spotlessApply
./gradlew --quiet checkEntryInteractionBoundaries
./gradlew --quiet verifySqlDelightMigration          # after SQLDelight changes
./gradlew --quiet testFossUnitTest
./gradlew --quiet :app:compileFossKotlin
git diff --check
```

Before release-shaped completion, also run the relevant CI sequence and the
telemetry/updater-enabled release build required by repository policy. Keep
output focused rather than emitting full Gradle logs.
