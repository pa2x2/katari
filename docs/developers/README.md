# Developer documentation

Katari's developer documentation covers the public Entry SDK and the extensions that consume it. It is intended for extension authors integrating with the SDK, rather than general app users.

## Build an extension

Extensions connect Katari to external catalogues. They provide entry metadata, child items, and media descriptors without bundling or hosting the content itself.

Start with the [extension development overview](../extensions/README.md), then create a source using the [getting-started guide](../extensions/getting-started.md). The public [`katari-extensions`](https://github.com/katariapp/katari-extensions) repository contains the distribution and contribution workflow for publicly maintained extensions.

## Understand the Entry SDK

The [`entry-source-api`](https://github.com/katariapp/katari/tree/main/entry-source-api) artifact is the authoring API for new Katari extensions. The SDK documentation is split by purpose:

- [SDK overview](./sdk/README.md) explains the source lifecycle and main contracts.
- [Data model](./sdk/data-model.md) documents entries, child items, media, pagination, and filters.
- [Content types](./sdk/content-types.md) explains how type and media determine app behavior.
- [Capabilities](./sdk/capabilities.md) documents optional source behavior and host expectations.
- [Compatibility and versioning](./sdk/versioning.md) separates SDK SemVer, loader families, app releases, and extension revisions.
- [Local SDK development](./sdk/local-development.md) covers coordinated changes between Katari and an extension.
- [SDK changelog](./sdk/changelog.md) records changes to the public contract.
- The [generated API reference](sdk/api/index.html) lists the complete public Kotlin surface.

Katari can load selected legacy Mihon extensions through compatibility code, but the legacy `source-api` module is not the authoring API for new extensions. See [extensions and compatibility](../differences/extensions-and-compatibility.md) or [migrate a Mihon source](../extensions/migrating-from-mihon.md) when working with an existing source.
