# BOOK content support decisions

Compact decision record for introducing `EntryType.BOOK`. It contains accepted
direction and remaining work, not discussion history or superseded alternatives.

## Locked decisions

| ID | Decision |
| --- | --- |
| BOOK-001 | Audiobooks are outside `BOOK` scope |
| BOOK-002 | BOOK uses pluggable content processors |
| BOOK-003 | DRM is deferred indefinitely and may never be supported |
| BOOK-004 | Sources describe resolved content with open identifiers |
| BOOK-005 | EPUB is the first processor target |
| BOOK-006 | Processors are initially built in, with a future reader-extension boundary |
| BOOK-007 | Missing/incompatible processors produce dedicated explanatory UI |
| BOOK-008 | Content descriptors include an optional profile |
| BOOK-009 | Media types are format identifiers |
| BOOK-010 | Initial EPUB scope is passive reflowable EPUB 2/3 |
| BOOK-011 | Multiple compatible processors require a rememberable chooser |
| BOOK-012 | Processors expose a format-neutral publication model |
| BOOK-013 | Katari owns the format-neutral public model |
| BOOK-014 | Locators have Katari-owned common fields plus processor extensions |
| BOOK-015 | Publication revisions reconcile progress without silent reset/misplacement |
| BOOK-016 | Locators use a balanced common field set |
| BOOK-017 | The latest explicit reading event wins progress conflicts |
| BOOK-018 | Processor API uses a mandatory core plus optional capabilities |
| BOOK-019 | The selected processor owns the complete reader UI |
| BOOK-020 | Processors obtain content through a Katari-owned access session |
| BOOK-021 | Content sessions expose capability-based resource access |
| BOOK-023 | Stable data models are separated from the built-in runtime SPI |
| BOOK-024 | Katari owns one content-session lease per invocation |
| BOOK-025 | Built-in SPI separates content, publication, and reader sessions |
| BOOK-026 | Readium Kotlin 3.3.0 is conditionally adopted as the EPUB engine |

## Scope and first processor

`BOOK` is readable content. Audiobooks are a separate listenable content type;
text-to-speech may still be an accessibility feature. DRM is out of scope for an
unspecified period. Protection remains distinct from representation, but no
hypothetical DRM integration expands the initial design.

The first built-in processor targets DRM-free, passive, reflowable EPUB 2 and 3:
ordinary XHTML/CSS, images, SVG, fonts, links, footnotes, navigation, language,
and writing direction. Fixed layout, scripting, media overlays, synchronized
narration, and embedded audio/video are excluded initially.

Readium Kotlin 3.3.0 is conditionally adopted as the private internal EPUB
engine after a successful host-side evaluation. Readium types must not cross the
EPUB processor boundary. Production integration remains gated by the conditions
recorded in [`book-readium-evaluation.md`](book-readium-evaluation.md).

## Processor architecture

```text
EntryType.BOOK
    -> BOOK interaction plugin
        -> resolved content descriptor
        -> processor selection
        -> processor-owned reader and session
```

Processors handle representation contracts, not normally websites. Sources own
discovery, authentication, updates, and retrieval. Selection applies to resolved
content, not permanently to an entry; merged entries may contain different
formats and sources.

Every processor declares support, resolves/validates a publication, owns its
reader, produces/restores locators, reports structured failures, and releases
resources. Download/cache, search, selection, annotations, preview, TTS, and
format settings are optional capabilities. Generic UI only exposes supported
actions.

The processor owns all reader rendering, controls, gestures, layout, and
format-specific interactions. Katari owns the format-neutral launch/session
boundary and persists reported location, progress, completion, time, errors, and
closure. A processor may reuse Katari components but no shared shell or Compose
ABI is required.

Only built-ins exist initially. Contracts nevertheless use stable identifiers,
do not depend on registration order or implementation class names, and leave a
credible future path to independently installed readers.

## Public data contracts

### Content descriptor

```text
BookContentDescriptor
├── format       open media type, e.g. application/epub+zip
├── profile      optional representation variant
└── protection   open identifier; initially none
```

Katari-defined formats use vendor media types. Profile is content metadata, not
processor API version. Content transport and credentials are not descriptor
fields.

### Content access

Processors receive a Katari-owned session containing stable content identity,
revision, descriptor, resource metadata, and cache/offline state. Resources have
publication/revision-stable identities suitable for persisted locators; each
session maps them to ephemeral URLs, handles, or transport requests. Access is
capability-based, including enumeration, streaming, ranges, and local
materialization where supported. Katari/source retain authentication, headers,
permissions, caching, cancellation, and lifecycle; processors never receive
source credentials or implementation access.

Katari creates and owns one content-session lease per reader, preview, indexing,
or background invocation. Nested resource handles must close before their parent
session. Closing the invocation cancels outstanding operations and releases the
session. Live sessions and handles are never serialized; process restoration
recreates them from stable content identity, revision, and persisted locators.
Durable downloads and caches outlive sessions through separate storage.

The built-in SPI has three scoped stages. `BookContentSession` provides
Katari-owned resource access. A selected processor opens it into a
`BookPublicationSession` containing the normalized publication and locator
operations. That session launches a processor-owned `BookReaderSession` which
emits state and errors. Close order is reader, publication, then content. Preview,
indexing, and background work may stop at the publication stage without creating
reader UI.

### Publication model

Every processor maps native content into Katari-owned, serializable types:

```text
BookPublication
├── identity and revision
├── descriptor
├── ordered readable resources
├── navigation tree
└── locators
```

This model serves packaged, serialized, single-document, paged, and future
formats. `Entry`/merge may retain source/library identity, while `EntryChapter`
may remain a synchronization or acquisition unit; neither defines universal
book structure.

### Locator

```text
BookLocator
├── resourceId          required
├── progression         optional within-resource progress
├── totalProgression    optional publication progress
├── logicalPosition     optional discrete position
├── fragments           optional precise references
├── boundedTextContext  optional relocation context
└── extensions          namespaced serialized processor values
```

Katari can display, back up, synchronize, and approximately recover common
location data. Processors use the complete locator for maximum precision and do
not invent common fields that are meaningless for their format.

### API stability boundary

Descriptors, publications, resources, navigation, locators, structured errors,
and capability identifiers form Katari-owned serializable models designed for
long-term compatibility. Processor, content-session, resource-handle, reader
launch, and lifecycle interfaces remain an evolvable in-process SPI while only
built-in processors exist. They should map cleanly to IPC concepts without
freezing an external Kotlin/Android ABI prematurely. A future reader-extension
protocol will reuse the stable models but define its packaging and IPC boundary
separately.

## Selection, errors, and persistence

Processor selection filters compatible and available candidates, uses a valid
remembered choice, automatically uses a sole candidate, asks with a “remember”
option when several remain, and returns a machine-readable reason when none do.
Installation never silently replaces a remembered reader.

Dedicated unsupported-content UI explains unknown format, unsupported profile,
version or protection, unavailable/incompatible processor, and invalid
descriptor. Screen versus dialog remains a UX detail.

BOOK reading state is keyed by publication identity/revision and stores locator,
progress, completion, last-opened time, and reading duration. Navigation may be
reconstructed or cached rather than fully materialized; annotations and sparse
per-resource state may use separate one-to-many storage later.

On publication change, the processor reconciles using resource identity,
structure, text context, and progress. Confident migration replaces the locator;
approximate migration informs the user; unresolved migration asks the user and
retains old state until resolved. Conflicts use the latest explicit reading
event, including intentional backward movement. Reading duration merges
separately and never selects location.

## Remaining work

1. Harden the capability-based content-session SPI beyond the evaluation's
   materialized-primary-resource path before production integration.
2. Satisfy the Readium production gates: effective Media3 validation, minified
   app/R8 build, final APK-size measurement, license attribution, hostile archive
   limits, and separately authorized device verification.
3. If reader extensions enter scope, define packaging, discovery, trust, loading,
   IPC/session transport, and ABI policy.
