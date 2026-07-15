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

Serialized prose sources expose each provider chapter as a separate entry child. Opening a chapter resolves only that chapter's normalized HTML; previous and next navigation opens adjacent stored chapters rather than combining the novel into an EPUB-style publication. The reader preloads only the immediate neighbors. Its chapter picker uses the already stored chapter metadata and resolves a chapter body only after selection.

When no compatible reader is available, Katari shows an unsupported-content screen instead of trying to open the publication in another media viewer. Support for additional book formats may be added through new readers in the future.

## Reader settings

Open **More → Settings → Reader** to configure the profile defaults for each installed reader. Each book processor can provide its own reader and settings, and Katari resolves every effective value in this order:

1. Entry override, when the setting supports one
2. Active-profile value
3. Processor default

Changing profiles therefore changes reader defaults without affecting other profiles. The layout mode is currently the only book-reader setting that can also be overridden for an individual entry. Changing it from the reader's appearance controls stores an override for that entry; the remaining settings are profile defaults.

| Reader | Layouts | Profile settings |
| ------ | ------- | ---------------- |
| Readium EPUB reader | Paginated or continuous scrolling | Color theme, font family and size, page margins, column count, publisher styles, line height, text alignment and normalization, tap navigation, and page-number display |
| Prose chapter reader | Paginated or continuous scrolling | Color theme, font family and size, page margins, line height, paragraph spacing, text alignment, tap navigation, and progress display |

Paginated reading is the default. The readers expose appearance and layout controls while reading, and paginated mode can use screen-edge taps for page turns. The EPUB reader navigates the publication's table of contents. The prose reader uses Katari's stored chapter list for its chapter picker and previous/next transitions while keeping only the current chapter and its immediate neighbors prepared.

## Reading progress

Book processors report progress as a format-neutral reading location rather than requiring every format to use pages or chapters. Katari stores that location for the active profile, entry, openable child, and publication resource. EPUB progress may target a resource inside an archive, while serialized prose progress belongs to one independently openable source chapter.
