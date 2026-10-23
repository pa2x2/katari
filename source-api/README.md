# source-api: compatibility with Mihon and Tachiyomi extensions

This module is **not Katari's extension SDK**. Katari's SDK is [`entry-source-api`](../entry-source-api), published with
`core:common` and `book-api`; new extensions are written against it.

`source-api` reproduces, inside Katari, the `eu.kanade.tachiyomi.source.*` classes that manga extensions built for
Mihon and Tachiyomi (Tachiyomi extensions-lib 1.6, Keiyoushi's extensions) were compiled against. Those extensions
ship without these classes and link against whatever the host app provides, so:

- what matters here is binary compatibility with already-built APKs, not API design;
- a public or protected member may only be removed or changed when [`abi/`](abi) says no supported extension
  links against it. `./gradlew verifyLegacySourceAbi` checks that;
- `source-compat` wraps these sources as Katari `UnifiedSource`s; new Katari code works with those, not with these types.

The HTTP helpers these extensions also call (`eu.kanade.tachiyomi.network.*`) live in `core:common`, because Katari
itself uses them and only one class of each name can exist in the app.

The Aniyomi counterpart is [`aniyomi-source-api`](../aniyomi-source-api).
