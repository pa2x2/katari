# BOOK content support research

Authoritative design record for `EntryType.BOOK`. It describes the accepted
architecture and invariants; implementation sequencing lives in
[`book-implementation-reference.md`](book-implementation-reference.md).

## Decision index

| Area | Decision IDs | Locked direction |
| --- | --- | --- |
| Scope and first format | BOOK-001–010 | Readable content only; DRM deferred; EPUB is first |
| Processor/runtime boundary | BOOK-011–021, 023–027 | Pluggable processors, Katari-owned models/sessions, processor-owned reader |
| Source/content boundary | BOOK-028–040 | Explicit `EntryMedia.Book`, lazy data-only resources, existing generic source gateway |
| Shared progress | BOOK-041–069 | Generic sparse progress, one-shot anime/manga migrations before BOOK |

The ranges preserve the decision history while the sections below are the
normative specification. Superseded alternatives are intentionally omitted.

## Scope and first processor

`BOOK` is readable content. Audiobooks are a separate listenable type; TTS may
later be an accessibility capability. DRM is deferred indefinitely and may never
be supported. Protection remains distinct descriptor metadata.

The first built-in processor handles DRM-free, passive, reflowable EPUB 2/3:
text, XHTML/CSS, images, SVG, fonts, links, footnotes, navigation, language, and
writing direction. Fixed layout, scripting, media overlays, embedded audio/video,
and synchronized narration are excluded initially.

Only built-in processors ship initially. Stable identifiers and data models must
nevertheless leave a credible future reader-extension/IPC path without freezing
the current in-process Kotlin or Compose SPI as an external ABI.

## Runtime ownership

```text
EntryType.BOOK
    -> BOOK interaction plugin
        -> resolved EntryMedia.Book
        -> processor selection
        -> BookContentSession
        -> BookPublicationSession
        -> processor-owned BookReaderSession
```

- Sources own discovery, authentication, updates, retrieval, and site-specific
  transformation.
- Processors own representation parsing, normalized publication output, complete
  reader UI, locator handling, failures, and cleanup.
- Katari owns processor selection, session lifecycle, progress, history,
  cache/download storage, and generic UI.
- Close order is reader, publication, then content. Sessions/handles are scoped,
  cancellable, idempotently closed, and never serialized.

Processors declare a mandatory core plus optional capabilities such as search,
selection, annotations, preview, TTS, download support, and format settings.
Generic UI exposes only supported actions. Processor selection automatically
uses a sole candidate, honors a valid remembered choice, and presents a chooser
with “remember” when several candidates remain. Missing or incompatible support
uses dedicated explanatory UI with structured reasons.

## Source SDK and entry screen

`UnifiedSource` remains the sole generic source gateway. `ImagePages` and
`Playback` remain first-class; BOOK adds explicit `EntryMedia.Book`. Source-child
resources continue resolving through `UnifiedSource.getMedia(chapter)`—there is
no additional BOOK fetch API.

Each `EntryMedia.Book` result is self-contained but not eager:

```text
EntryMedia.Book
├── descriptor: media type, optional profile, protection
├── optional publication-key override
├── catalog + catalog revision + coverage
├── optional hierarchy hints
├── optional initial resource
└── resolved initial-resource location
```

The default publication identity is `Entry.source + Entry.url`; an override is
only for one entry containing multiple logical publications. Catalog and
resource revisions are independent. Publications may be packaged, lazy,
ongoing, partially enumerated, partially accessible, or partially cached.

Catalog facts may include stable IDs, titles, order, grouping, and child
mappings. The processor validates and normalizes them. Persisted `resourceId` is
separate from retrieval identity (`SEntryChapter.url`); database chapter IDs are
runtime-only.

Source resource locations form a closed data-only set:

- `SourceChild(resourceId, sourceChildKey)`;
- `RemoteRequest(url, headers)`;
- bounded `InlineText` and `InlineBytes`;
- validated `LocalUri` or app-issued reference.

Resolution cannot loop on the same source child. Large packages are not inline,
arbitrary filesystem paths are invalid, and credentials never reach processors.
Package-internal files remain processor concerns.

`book-api` owns shared stable semantics and is published transitively.
`entry-source-api` owns `EntryMedia.Book`, source resource descriptions, and
locations; equivalent BOOK descriptors are not duplicated.

The entry screen displays source `EntryChapter` acquisition/access units; the
reader displays normalized publication navigation. Gutenberg-like rows may be
EPUB/HTML/text renditions whose internal chapters exist only in the reader.
Web-novel chapters may be both entry rows and publication resources. Catalog
presence never proves entitlement.

Availability is contextual and structured: unknown, available, authentication
or purchase required, unsupported app-only access, removed, or region-restricted.
Downloads are resource-scoped.

## Processor-facing data and access

`BookContentSession` exposes stable identity/revision and a cursor-paged catalog.
Resources report metadata, revision, cache/availability state, and `STREAM`,
`RANGE`, or `MATERIALIZE` capabilities. Access returns scoped handles; durable
caches outlive sessions.

Processors emit Katari-owned serializable models:

```text
BookPublication                    BookLocator
├── identity and revision          ├── resourceId
├── descriptor                     ├── progression / totalProgression
├── readable resources             ├── logicalPosition
└── navigation tree                ├── fragments / bounded text context
                                    └── namespaced extensions
```

Locators contain only meaningful common fields plus processor extensions.
Revision reconciliation uses stable resource identity, structure, text context,
and progress. Confident relocation is automatic; approximate relocation informs
the user; unresolved relocation asks and retains old state. The newest explicit
locator event wins, including deliberate backward movement.

## Shared progress architecture

BOOK does not introduce isolated persistence. Generic `entry_progress_state`
replaces anime `playback_state` and manga `chapters.last_page_read` before BOOK
depends on it.

```text
entry_progress_state
├── entry_id; optional chapter_id mapping
├── content_key; resource_key; optional resource revision
├── locator kind; position; extent
├── progression; total progression; namespaced extension JSON
├── completed
├── locator_updated_at
└── completion_updated_at
```

### Identity and lifecycle

- The owning entry FK cascades. The nullable child FK uses `ON DELETE SET NULL`;
  stable progress survives temporary child removal.
- Empty `content_key` means the entry's default content. Only exceptional
  multi-publication BOOK media supplies a discriminator.
- Manga/anime use `EntryChapter.url` as `resource_key`; BOOK uses stable
  publication resource IDs. Backup portability adds source ID and entry URL.
- Rows are sparse: partial/completed state, extensions, and explicit reset
  tombstones exist; untouched resources do not.
- Completion is authoritative here and transactionally projected to
  `EntryChapter.read` when mapped. Child bookmarks and future precise BOOK
  bookmarks/annotations remain separate.

### Locator and conflict rules

- Canonical SQL columns hold common locator values; namespaced JSON holds only
  type-specific extensions. There is no duplicate full locator blob.
- Locator kinds are open: manga uses zero-based `page`, anime uses millisecond
  `time`, and BOOK maps its common fields plus precise extensions.
- Unknown kinds/extensions survive backup, restore, and copy. Generic fields
  remain visible, while Resume reports unsupported locator until an interaction
  can interpret it.
- Newest `locator_updated_at` chooses position; newest `completion_updated_at`
  chooses completion. Exact clock ties retain existing local state.
- Reset stores empty locator/incomplete state with new clocks so old state cannot
  resurrect. Manga/anime preserve reset-to-start behavior; BOOK retains precise
  location on mark-unread unless the user explicitly resets progress.
- Each interaction owns automatic completion policy; persistence never infers it
  from progression.

### History and interaction boundary

History remains separate and authoritative for actual consumption time/duration,
independent clearing, and optional backup. Initial BOOK history remains backed
by the mapped web-novel child or selected packaged rendition child. Resume reads
progress, not history.

`EntryProgressInteraction` owns generic snapshot, restore, and mapped-copy.
Backup, merge, Continue, library progress, and child labels use it rather than
repositories directly. Anime playback preferences become a separate interaction.

### Migration rules

Migration phases are foundation, anime, manga, then BOOK. Anime and manga each
use an atomic one-shot backfill and immediate cutover: no dual-write and no
legacy-read fallback. Anime removes `playback_state`; manga rebuilds `chapters`
without `last_page_read`.

Only meaningful legacy rows migrate. Native page/time units are preserved;
unknown extents/progressions remain absent. Trustworthy consumption timestamps
seed clocks; metadata time and migration time never pretend to be user events.
Recoverable invalid values are deterministically normalized before insertion.
SQL enforces key, range, clock, and uniqueness invariants.

New backups write generic progress only. Restore indefinitely accepts legacy
manga `lastPageRead` and anime playback payloads; field numbers remain reserved.
When generic and legacy payloads coexist, generic state wins.

## Readium EPUB evaluation

Readium Kotlin 3.3.0 is conditionally adopted as the private EPUB engine. The
host spike proved EPUB 2 NCX and EPUB 3 navigation, RTL/language mapping, nested
anchors, Katari identity/revision ownership, locator serialization/restoration,
structured malformed-content failure, and reverse-order lifecycle cleanup.
Readium imports remain confined to `:entry-interactions:book`. The built-in
processor is now wired through the generic BOOK host to a processor-owned EPUB
reader Activity, generic progress/history persistence, and ordered session
cleanup. Production hardening and separately authorized device validation remain.

Evaluation facts:

- selected artifacts are `shared`, `streamer`, and `navigator` 3.3.0;
- direct AARs total 2,975,665 compressed bytes before R8/overlap;
- no native libraries, manifest components/permissions, consumer R8 rules, LCP,
  or DRM binary were found;
- Readium is BSD-3-Clause; embedded PhotoView/font attribution needs verification;
- navigator and Katari explicitly align on Media3 1.10.0;
- Readium's navigator AAR embeds PhotoView 2.3.0 while Katari already owns that
  dependency for manga and cover reading. A scoped artifact transform removes
  only Readium's duplicate copy, preserving the existing generic interaction
  dependency direction;
- the telemetry/updater-enabled minified release assembly passes with the
  built-in processor and reader runtime.

Remaining production adoption work includes final APK-size comparison, complete
attribution, hostile-archive limits, unsupported EPUB fixtures, and separately
authorized device checks for rendering, pagination, gestures, lifecycle,
restoration, and resource loading.

## Deferred scope

- DRM and audiobooks;
- reader-extension packaging, discovery, trust, loading, IPC/session transport,
  and ABI policy;
- annotations and precise reader bookmarks;
- additional formats/processors after EPUB.
