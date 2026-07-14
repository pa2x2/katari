# Book API architecture

BOOK has two public boundaries that change for different reasons:

- `entry-source-api` defines how an extension discovers entries, lists source children, and describes where Katari can retrieve publication resources.
- `book-api` defines the processor-neutral data exchanged after Katari has taken ownership of resource access: processor selection, normalized publication structure, reading locations, and structured failures.

An extension depends only on `entry-source-api`. That artifact exposes `book-api` transitively at the same SDK version.

## Why it is separate

A book format is not just another source capability. EPUB, a web novel, a fixed-layout publication, and a future format may need different parsing engines, resource behavior, navigation models, and reader UI. Those implementations should be replaceable without changing `UnifiedSource` or teaching every source about every reader.

Keeping the stable book contract separate provides three boundaries:

```text
Extension source
    └── entry-source-api
            └── describes source-owned book resources
                    ↓ Katari-owned access session
                book-api
                    └── format processor and processor-owned reader
```

This prevents a format processor from depending on extension loading, source HTTP classes, or a concrete reader. It also leaves room for reader extensions later: the data boundary does not require processors to remain built in.

Katari currently registers book processors as built-in application components. `book-api` is a stable data boundary, not a public processor lifecycle, UI, or installation API, so third-party extensions cannot add book processors or readers yet.

## Artifact responsibilities

`entry-source-api` owns source-facing contracts:

- `EntryType.BOOK` and `EntryMedia.Book`;
- `BookResourceCatalog` and `BookSourceResource`;
- the closed `BookResourceLocation` retrieval descriptions;
- optional source-provided resource hierarchy and initial-resource selection.

`book-api` owns data shared by the generic BOOK host and format processors:

- `BookContentDescriptor` for open format, profile, and protection identifiers;
- processor-facing resource metadata and access capabilities;
- normalized `BookPublication`, reading order, and navigation;
- `BookLocator` and bounded text context for persistent progress;
- structured availability, cache, and failure values.

!!! note

    `book-api` deliberately contains no Android UI, network client, source callback, EPUB parser, DRM implementation, or reader engine. EPUB support and its reader live in the built-in EPUB processor, outside the public data contract.

## Access and trust boundary

Sources return data-only `BookResourceLocation` values. Katari validates them and owns network, URI, cache, range, and materialization access. A processor requests resources through the host session; it does not call the source or receive source credentials directly.

!!! warning

    This boundary keeps format code independent from source implementations and makes resource limits enforceable in one place. It does not bypass access controls. DRM processing is outside the current scope, and `protection` is selection metadata rather than permission to remove or circumvent protection.

## Versioning

`book-api` and `entry-source-api` are released from the same Katari commit under the same `sdk-*` tag. Changes to either artifact count as Entry SDK changes and participate in the same Semantic Versioning decision, changelog, documentation build, and JitPack verification.

For local development, publish the complete SDK rather than publishing either artifact alone. See [Local SDK development](./local-development.md).

The generated [Book API reference](api/book/index.html) is the source-level reference for processor-neutral book models.
