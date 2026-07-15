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

Serialized prose sources expose each provider chapter as a separate entry child. Opening a chapter resolves only that chapter's normalized HTML; previous and next navigation opens adjacent stored chapters rather than combining the novel into an EPUB-style publication.

When no compatible reader is available, Katari shows an unsupported-content screen instead of trying to open the publication in another media viewer. Support for additional book formats may be added through new readers in the future.

## Reader settings

Each book processor can provide its own reader and settings. Every setting declares whether it is a profile default or can also be overridden for an individual entry. Katari resolves an effective value in one place using this order:

1. Entry override, when the setting supports one
2. Active-profile value
3. Processor default

Changing profiles therefore changes reader defaults without affecting other profiles. An entry override remains attached to that entry and takes precedence until it is cleared.

## Reading progress

Book processors report progress as a format-neutral reading location rather than requiring every format to use pages or chapters. Katari stores that location for the active profile, entry, openable child, and publication resource. EPUB progress may target a resource inside an archive, while serialized prose progress belongs to one independently openable source chapter.
