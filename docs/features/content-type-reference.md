# Content type support

This reference compares user-facing capabilities that each content type implements or explicitly supports. Functionality supplied automatically by the shared entry model, such as appearing in the unified library and global search, is not repeated here. Features inherent to one kind of media, such as video stream or subtitle selection, are documented with that media's experience instead of being shown as unavailable for other types.

Status meanings:

- **✓ Supported**
- **◇ Source-dependent** — requires support from the source
- **— Not available**

## Entry interactions

| Capability                                         | Manga | Anime | Book |
| -------------------------------------------------- | :---: | :---: | :--: |
| Continue from saved progress                       |   ✓   |   ✓   |  ✓   |
| Mark individual child items consumed or unconsumed |   ✓   |   ✓   |  ✓   |
| Show partial progress for individual child items   |   ✓   |   ✓   |  ✓   |
| Apply smart library-update restrictions            |   ✓   |   ✓   |  ✓   |
| Merge versions from different sources              |   ✓   |   ✓   |  —   |
| Bookmark individual child items                    |   ✓   |   —   |  —   |
| Show gaps between missing child items              |   ✓   |   —   |  —   |
| Filter child items by release group                |   ✓   |   —   |  —   |
| Migrate an entry to another source                 |   ✓   |   —   |  —   |

## Downloads

| Capability                                          | Manga | Anime | Book |
| --------------------------------------------------- | :---: | :---: | :--: |
| Download individual child items for offline use     |   ✓   |   ✓   |  —   |
| Bulk-download child items                           |   ✓   |   ✓   |  —   |
| Automatically download newly discovered child items |   ✓   |   ✓   |  —   |
| Delete downloads after marking an item consumed     |   ✓   |   —   |  —   |

!!! note

    Bookmark-based bulk downloads are available when the content type supports individual bookmarks.

## Discovery and integrations

| Capability                                      | Manga | Anime | Book |
| ----------------------------------------------- | :---: | :---: | :--: |
| Preview entries while browsing                  |   ✓   |   ◇   |  —   |
| Use immersive browsing                          |   ◇   |   ◇   |  —   |
| Import content through the bundled Local source |   ✓   |   —   |  —   |
| Use supported legacy Mihon extensions           |   ✓   |   —   |  —   |
| Connect entries to tracking services            |   ✓   |   —   |  —   |

Source-provided capabilities and available catalogue content can vary by extension. See [Unified library](unified-library.md), [Book reading](book-reading.md), [Video playback](video-playback.md), and [Extensions and compatibility](../differences/extensions-and-compatibility.md) for details beyond this comparison.
