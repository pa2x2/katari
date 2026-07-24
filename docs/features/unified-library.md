# Unified library

Katari stores different kinds of stories as **entries** in one library. Every entry has a type and contains the units that can be opened for that kind of content.

The type controls the experience without splitting the app into unrelated libraries:

- Each type opens with its appropriate reader, player, or other interaction.
- Library, updates, history, search, categories, and sources can contain multiple types together.
- Type badges identify entries when the layout would otherwise be ambiguous.

## Organize the library

Open the library display settings and use **Group** to choose how pages are built:

- **Category** uses the familiar category tabs.
- **Type** creates a page for each entry type present in the library.
- **Extension** normally groups entries by source and labels each page with the source name. An extension that provides several sources can therefore produce several pages. A merged entry whose members span multiple sources appears on a **Multi** page.
- **Type → category** and **Category → type** create nested pages in the selected order.
- **Extension → category** and **Category → extension** provide equivalent source-based combinations.

The active page gives context to actions such as updating a category, type, or source. Filters, sorting, grid/list display, and badges still work within the resulting pages.

## Entry identity

Katari identifies an entry by its profile, source, source URL, and type. Entries of different types returned at the same URL are therefore distinct. A source should set the correct type when it publishes catalogue results.

Changing an entry's source identity or URL can make Katari treat it as new. This is why source migrations and extension updates should preserve stable identities.

## Shared and type-specific behavior

Common metadata, categories, favorites, updates, and history use the shared Entry model. Opening, progress, settings, and downloads remain type-specific, so behavior configured for one interaction does not automatically apply to another.

Library-update notifications are grouped and worded for the updated entry type. Their actions reflect the behavior that
type currently provides: opening a child, marking it consumed, and downloading it appear independently. Tapping a child
notification still opens the entry details when that type does not provide direct child opening.

Download controls likewise follow the content type's actual download support and the participating sources. Local or
missing sources do not expose remote-download actions. Bulk actions operate on the children currently visible after
entry filters are applied; reader navigation preferences do not change that selection.

See [Content type support](content-type-reference.md) for a comparison of cross-type capabilities.

For entries from multiple sources that represent the same title, see [Merged entries](./merged-entries.md).
