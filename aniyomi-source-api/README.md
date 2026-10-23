# aniyomi-source-api: compatibility with Aniyomi extensions

This module is **not Katari's extension SDK**. Katari's SDK is [`entry-source-api`](../entry-source-api); new
extensions, anime ones included, are written against it.

This module reproduces, inside Katari, the `eu.kanade.tachiyomi.animesource.*` and `eu.kanade.tachiyomi.torrentutils.*`
classes that Aniyomi anime extensions were compiled against. Those extensions ship without these classes and link
against whatever the host app provides. It is the Aniyomi counterpart of [`source-api`](../source-api), which does the
same for Mihon and Tachiyomi manga extensions.

## One surface for every library version

Aniyomi extensions are built against Aniyomi's extensions-lib 14, 16 or 17, or against Komikku's fork of it, which
Yūzōnō's catalogue uses. The library version an extension declares doesn't say which classes it calls: Yūzōnō's
"lib 14" extensions call lib 16 classes such as `Hoster`. So this module holds the union of all of them:

- members a newer library dropped or replaced stay, with a default where the older library had none;
- constructors and `copy` overloads of older `Video` and `Hoster` shapes exist in bytecode only (hidden from Kotlin);
- Komikku's related-anime calls sit beside extensions-lib 17's;
- the Rx `fetch*` calls of extensions-lib 14 are left out: no extension in Yūzōnō, salmanbappi or Secozzi's catalogues
  calls or overrides them (measured 2026-10-23).

Because the point is binary compatibility with already-built APKs, a public or protected member may only change when
no supported library declares it.

## What lives elsewhere

- The HTTP helpers extensions call (`eu.kanade.tachiyomi.network.*`) are in `core:common`, shared with manga
  extensions, because only one class of each name can exist in the app.
- `source-compat` turns these sources into Katari `UnifiedSource`s. Only it and the extension loader in `app` use these types.
- The extension loader takes the class-loading rule from `mihon.aniyomi.runtime.AniyomiExtensionClassLoading`.

Most of the code is ported from Aniyomi's `source-api` (Apache License 2.0) and Anikku's related-anime defaults
(Apache License 2.0).
