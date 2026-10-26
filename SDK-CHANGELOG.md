# SDK Changelog

## [3.0.0] - Unreleased

> From SDK 2.x, [Upgrade to SDK 3.0](https://pa2x2.github.io/katari/docs/extensions/upgrade/3.0/) walks through the changes.

### ✨ Added

- `UnifiedSource.refresh` fetches an entry's details, its chapters, or both in one call, and receives the stored chapters.
- `MergedEntrySource.getMergedEntries` and `SEntry.merged` let a source present several entries, such as seasons, as one merged entry whose members users can't remove.
- `SEntry.backgroundUrl` supplies wide background art, and `SEntry.UPCOMING` marks a title that is announced but not released.
- `SEntryChapter.summary`, `previewUrl` and `kind` add a synopsis, a still image and a filler marker to chapters and episodes.
- `PlaybackDescriptor.servers` and `PlaybackSelection.serverKey` add servers as a playback choice that Katari falls back through.
- `VideoStream` gains `height`, `bitrate`, `subtitles`, `audioTracks`, `markers` and `mpvOptions`.
- `VideoAudioTrack` describes an external audio track for a stream.
- `PlaybackMarker` marks intros, outros, recaps, previews and chapters on a video's timeline.
- `DeferredStreamSource.resolveStream` resolves a stream's link only when it is about to play or download.
- `SEntryChapterGroup`, `SEntryChapter.group` and `EntryRefresh.chapterGroups` put chapters into volumes, arcs or other parts of the story.
- `ChapterListPartsSource` and `EntryRefresh.rest` let a source give a long chapter list in parts, which Katari reads in the background.
- `SEntryChapter.dateText` shows a release time the site gives only as text, such as "3 days ago".
- `EntryCatalogueSource.textSearchUsesFilters` tells users that the site's text search ignores the filters.
- `ArtworkSource` loads covers and chapter stills with the source's client and headers, and `EntryHttpSource` has it.
- `SiteStorageSource` receives the site's `localStorage` and `sessionStorage` after the user browses it in WebView.
- `EntryMedia.Book.linkedResources` lets prose chapters name images by URL or `data:` URI, which Katari fetches and downloads.
- `EntryMedia.Book.styleSheets` applies one style sheet to every HTML chapter of a book.
- Prose chapters use their `@font-face` fonts and AVIF images.
- Prose chapters keep boxes, gradients, shadows, flex and grid rows and floats, read with full CSS selectors, variables and `@media`.
- Book documents gain `BookDocumentBox`, side-by-side rows, floats, justified text, letter spacing, case changes, small caps and text shadows.
- Suspend `OkHttpClient.get` and `OkHttpClient.post` wait for a successful response.
- Requests to local network hosts without the permission fail at once with `LocalNetworkAccessDeniedException`.

### 🔄 Changed

- `RelatedEntriesSource.getRelatedEntries` returns `RelatedEntryGroup`s, each with an optional title.
- `VideoStream.request` is nullable for streams a `DeferredStreamSource` resolves later.
- The new `servers` and `serverKey` parameters change the parameter order of `PlaybackDescriptor` and `PlaybackSelection`.
- `SEntry` and `SEntryChapter` have new abstract members.
- `BookDocumentStyle.flow`, `BookDocumentInlineStyle.textContext` and `BookDocumentImage.decorative` are constructor parameters.
- `InlineText` holds up to 4 Mi characters and `InlineBytes` up to 12 MiB.
- Katari reads a book resource's media type from its bytes when the source declares none.
- `JavaScriptEngine` is an abstract class that extensions get from Katari, and its scripts run Promise jobs.
- The SDK no longer depends on a JavaScript engine.
- Source preferences follow the user's profile, including in requests made on OkHttp's threads.
- Injekt is read-only for extensions and resolves only the services Katari shares with them.
- The SDK depends on `uy.kohesive.injekt:injekt-core` 1.16.1 instead of `com.github.mihonapp:injekt`.
- Katari loads `kotlin.*` classes from the app instead of the extension.
- `core-common` no longer depends on Katari's `i18n` module.

### 🗑️ Removed

- `UnifiedSource.getContentDetails`, `UnifiedSource.getChapterList` and `IncrementalChapterSource`; use `refresh`.
- `SubtitleSource`; subtitles come with each `VideoStream`.
- `BookDocumentStyle.withFlow`, `BookDocumentInlineStyle.withTextContext` and `BookDocumentImage.withAccessibility`.
- `BookDocumentStyle.backgroundArgb`, `border` and `paddingEm`; they belong to the `BookDocumentBox` around the block.
- The `stringResource`, `pluralStringResource` and `toast(StringResource)` helpers in `core-common`.
- The top-level `launchUI`, `launchIO` and `launchNow` helpers; the `CoroutineScope` versions remain.
- `EntryCatalogueSource.supportsImmersiveFeed`; the immersive view works with every source's manga and anime.

## [2.6.0] - 2026-09-06

### ✨ Added

- `EntryDateFilter` extends `EntryFilter.Text` with typed `dateValue` access, allowed precision, minimum and maximum dates, and required-field validation. `EntryPartialDate` preserves year-only, year-month, or full-date values when parsing and serializing, and rejects impossible calendar dates.
- Optional `EntryFilterMetadataProvider` and `EntryFilterMetadata` give filters stable IDs, descriptions, constraint or ordering roles, and stable option IDs. Aliases, legacy names, and historical option IDs let sources preserve saved selections across renaming, regrouping, and option reordering. Filter IDs and aliases must be unique across the source's filter tree.
- `EntryFilterValueMigration` lets sources version and migrate encoded text or paged-filter state, returning `null` for incompatible saved values.
- `EntryFilterValidator`, `validationIssues()`, and `requireValidFilters()` support field and group validation with structured issues. Identity validation detects ambiguous filter IDs, invalid option IDs, and invalid selections before presets are saved or filters are dispatched.
- `EntryFilterStateSemantics` lets sources define active-selection counts and clearing behavior for custom values. `EntryFilterGroupSummary` supplies optional text for collapsed groups independently of their selection counts.
- `BookDocumentLinkTarget.Resource` links to another document in the same publication, with an optional fragment. `BookDocumentLinkTarget.Reference` describes contextual references that can open without replacing the primary reading position.
- `BookDocumentFlowStyle` and `BookDocumentStyle.withFlow()` carry block spacing, line height, first-line indentation, text direction, and language. `BookDocumentTextContext` and `BookDocumentInlineStyle.withTextContext()` add language and direction to inline ranges without requiring visual styling.
- `BookDocumentImage.withAccessibility()` supports explicitly decorative images through the `decorative` flag. Decorative images cannot also provide alternative text.
- `BookDocumentPublicationProgress.totalProgression()` calculates progress across ordered documents weighted by their logical text length.

### 🔄 Changed

- `BookDocumentPublicationModel.DESCRIPTOR` now advertises `book.document` version `2`. The expanded style and image models retain SDK 2.5 constructor, copy, component, and serialization-constructor signatures.
- `BookDocumentInlineStyle` now allows an empty base for adding text context. `BookDocumentInlineStyleRange` rejects styles with neither visual effects nor text context.

## [2.5.0] - 2026-08-01

### ✨ Added

- Included unified source APIs for popular entries, latest updates, search, entry details, chapter lists, and media retrieval across manga, anime, and books.
- Included optional source capabilities for home-page sections, entry previews, related entries, source preferences, image requests, entry and chapter URI resolution, and subtitles.
- Included filter autocomplete and searchable options, plus paged filter groups with source-provided search, separate available and selected collections, bidirectional pagination, and navigation to source-defined destinations.
- Included BOOK content descriptors, resource catalogs, availability and access metadata, and resource locations for source chapters, HTTP requests, inline content, local content URIs, and app-owned references.
- Included shared BOOK publication and document models for structured text, styles, images, tables, links, table-of-contents navigation, and reading positions.

[3.0.0]: https://github.com/pa2x2/katari/tree/sdk-3.0.0
[2.6.0]: https://github.com/pa2x2/katari/tree/sdk-2.6.0
[2.5.0]: https://github.com/pa2x2/katari/tree/sdk-2.5.0
