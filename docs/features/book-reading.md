# Book reading

Katari selects a reader from the publication format reported by the source. Format support belongs to the reader, so a source may offer a BOOK entry even when the installed app has no compatible reader for one of its publications.

## Built-in format support

| Format | Built-in reader | Support |
| ------ | --------------- | ------- |
| Reflowable EPUB 2 | Readium EPUB reader | Supported |
| Reflowable EPUB 3 | Readium EPUB reader | Supported |
| Serialized HTML prose chapter | Prose chapter reader | Supported |
| Fixed-layout EPUB | — | Not supported |
| DRM-protected EPUB | — | Not supported |
| PDF and other document formats | — | Not supported |

EPUB publications must be supplied as `application/epub+zip`. If a source does not declare a layout, Katari inspects the publication while opening it and accepts it only when it is reflowable.

Serialized prose sources expose each provider chapter as a separate entry child. Opening a chapter resolves only that chapter's normalized HTML; previous and next navigation opens adjacent stored chapters rather than combining the novel into an EPUB-style publication. An adjacent chapter body is resolved only after its transition becomes the active reading item, or after the chapter is selected explicitly. Loading failures and retry remain inline with that transition. The chapter picker uses the already stored chapter metadata and resolves a chapter body only after selection.

The prose reader preserves authored structure in scrolling and paginated modes, including thematic breaks, ordered and nested lists, whitespace-sensitive passages, figures and captions, bounded tables, internal notes, safe external links, native disclosure blocks, and a reviewed subset of meaning-bearing colors, borders, alignment, and fonts. Ordinary styled prose remains in the continuous text flow; only structural content and styled panels that require atomic rendering receive dedicated pages. Images and custom fonts are fetched through Katari's controlled resource path rather than by a WebView. Images are sampled to their rendered bounds and released with the figure that owns them, while missing images retain readable alternative text.

Audio, video, embedded webpages or objects, canvas/SVG, mathematics, and specialized charts are not rendered. Each outermost unsupported content block appears as `-- unsupported content block --` so unsupported material is visible and can be investigated without executing or fetching it.

When no compatible reader is available, Katari shows an unsupported-content screen instead of trying to open the publication in another media viewer. Support for additional book formats may be added through new readers in the future.

## Reader settings

Open **More → Settings → Reader** to configure the profile defaults for each installed reader. Each book processor can provide its own reader and settings, and Katari resolves every effective value in this order:

1. Entry override, when the setting supports one
2. Active-profile value
3. Processor default

Changing profiles therefore changes reader defaults without affecting other profiles. Settings that support an entry override can also be changed for an individual entry from the reader's controls.

Each reader exposes its currently available appearance, layout, and navigation controls in the app. The EPUB reader navigates the publication's table of contents. The prose reader uses Katari's stored chapter list for its chapter picker and previous/next transitions while keeping the current chapter and any already-requested immediate neighbors prepared.

Use **Reset** from a reader settings dialog to remove that reader's active-profile values and the current entry's reader overrides. Reading progress, bookmarks, and downloads are not changed.

## Offline downloads

Download chapters for offline reading from the book's entry screen:

- Use the download button beside a chapter to download it individually.
- Choose **Next N** or **Unread** from the download menu to download multiple chapters.
- Manage queued and saved chapters from the Downloads screen.

Downloaded chapters open without a network connection. Prose downloads include every image and custom font referenced by the normalized supported content; Katari does not publish the download as complete if one of those required assets cannot be verified. Dependency discovery and packaging use the same primary bytes. Resource acquisition is bounded before and during each read by the renderer's media limit and the package's remaining byte budget, while dependency count and cumulative encoded bytes are bounded across the package. Malformed, dishonest, or changing resources therefore cannot turn a rejected dependency into an unbounded intermediate download. Automatic downloads follow your existing download settings.

## Reading progress

Book processors report progress as a format-neutral reading location rather than requiring every format to use pages or chapters. Katari stores that location for the active profile, entry, openable child, and publication resource. EPUB progress may target a resource inside an archive, while serialized prose progress belongs to one independently openable source chapter.

When migrating a book to another source, Katari carries saved progress to the matched target item and reconciles it when that target publication is first opened. Compatible locations are restored directly; otherwise prose uses its portable progression and EPUB uses the closest position exposed by the target publication. If the target reader cannot produce a valid location, consumed state is still preserved but the target item opens from the beginning.
