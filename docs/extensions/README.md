# Extension development

Katari extensions connect the app to external catalogues. They provide metadata and tell Katari how to resolve something a person can read or watch. Extensions do not bundle or host the content itself.

Katari uses [`entry-source-api`](https://github.com/katariapp/katari/tree/main/entry-source-api) for new extensions. Although Katari can still load compatible [Mihon](https://mihon.app/) extensions, new extensions should not depend on the legacy `source-api` module or use its `SManga`, `SChapter`, `MangasPage`, `Page`, or RxJava contracts.

The public [`katari-extensions`](https://github.com/katariapp/katari-extensions) repository contains Katari extensions and their contribution workflow. Use it as the starting point when adding or maintaining an extension intended for public distribution.

## The model

The manifest entry point can be a `UnifiedSource` implementation or an `EntrySourceFactory` that creates one or more sources. A factory is useful when one extension exposes multiple sources:

```text
UnifiedSource
    ├── SEntry             a title in a catalogue
    ├── SEntryChapter      something the user can open
    └── EntryMedia         representation of the content

EntrySourceFactory
    └── one or more UnifiedSource instances
```

The source is not permanently classified by content type. Each `SEntry` declares its `EntryType`, and `getMedia()` returns the payload Katari should open. One factory can expose several sources, and a source may return different entry types in the same catalogue.

## Choose a starting point

- Extend `EntryImageHttpSource` for an HTTP source that resolves image pages.
- Extend `EntryHttpSource` for an HTTP source that resolves video or other non-image media.
- Implement `UnifiedSource` directly when the provided HTTP behavior is not appropriate.

## Guides

1. [Create your first extension](./getting-started.md)
2. [Understand the Entry SDK](../developers/sdk/README.md)
3. [Make HTTP requests and parse responses](./http-and-parsing.md)
4. Choose the [image](./image-media.md) or [playback](./playback-media.md) media cookbook.
5. [Publish and maintain the extension](./publishing.md)

If you already maintain a Mihon source, follow the [migration guide](./migrating-from-mihon.md). Before selecting an SDK release, read [SDK compatibility and versioning](../developers/sdk/versioning.md).

The [SDK documentation](../developers/sdk/README.md) explains the public contracts and links to the generated API reference. The Kotlin sources under [`entry-source-api`](https://github.com/katariapp/katari/tree/main/entry-source-api/src) remain the source of truth for development snapshots.
