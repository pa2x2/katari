# Publishing and maintaining extensions

An extension update must be recognizable as the same Android package, signed by the same publisher, and compatible with Katari's Entry API family. It should also preserve source and content identities so an upgrade does not duplicate a user's library or lose history.

For extensions distributed through Katari's public repository, follow the contribution and release workflow in [`katari-extensions`](https://github.com/katariapp/katari-extensions).

## Before the first release

Choose values that can remain stable:

- A unique Android `applicationId` such as `eu.kanade.tachiyomi.extension.en.example`.
- A signing key that is backed up securely and never committed to the repository.
- A source `name`, `lang`, and `versionId`, or an explicit source `id`.
- Stable entry and chapter URL formats.

The APK manifest must declare the `tachiyomi.extension` feature, its source or factory class, and its adult-content classification. The supported Entry API family comes from the first two components of Android `versionName`. See [getting started](./getting-started.md#declare-the-extension).

## Version the APK

Increase Android `versionCode` for every published update. Katari and Android use it to decide which package is newer; a lower version code cannot replace a higher installed version.

The extension `versionName` has three numeric components:

```text
<Entry API major>.<Entry API minor>.<extension revision>
```

For example, an extension targeting API family `<major>.<minor>` starts with `<major>.<minor>.1`; its next extension release is `<major>.<minor>.2` even when the SDK dependency is unchanged. Katari and the extension repository derive the compatibility family by removing the final component from `versionName`.

Do not treat `versionName` as an independent SemVer for the extension. Read [API compatibility and versioning](./api-compatibility.md) before changing either of its first two components.

## Preserve Android identity

Updates must use the same `applicationId` and signing certificate. Changing either makes the APK a different or untrusted installation. Protect the signing key and document its recovery procedure outside the public repository.

Extensions distributed by a configured store are trusted through the store's signing fingerprint, so a same-signed update should not require another decision. Trust granted manually to an ad-hoc APK is version-specific; Katari asks again after its `versionCode` changes even when the signing certificate is unchanged. A changed fingerprint still indicates a different signing key.

## Preserve source identity

`EntryHttpSource` generates its source ID from lowercased `name`, `lang`, and `versionId`. Changing any of these can create a new source and disconnect existing library entries from their implementation.

When a public name must change, preserve the old ID explicitly:

```kotlin
override val id: Long = 1234567890123456789L
override val name: String = "New display name"
```

Record the existing ID before making the change. Do not invent a replacement number after publishing. Moving a source between factories is safe only when its package, class loading, and source ID remain compatible.

## Preserve content identity

Inside a source, entry and chapter URLs are persistent identifiers, not merely navigation links.

- Keep an entry's `url` stable when its title, cover, or provider domain changes.
- Keep chapter URLs stable when labels or numbering change.
- Prefer domain-free paths with `setUrlWithoutDomain()` when the path is stable.
- Retain query parameters when the provider uses them as identifiers.
- Resolve temporary tokens and media URLs at request time; never use them as content identity.

If a provider changes its identifiers, implement an intentional migration strategy before publishing. Merely returning new URLs can duplicate entries and discard the connection to history and downloads.

## Maintain a released source

For every update:

1. Build against a tagged SDK, not `local-SNAPSHOT`.
2. Increase `versionCode` and the final extension-revision component of `versionName`.
3. Build with the release signing configuration.
4. Install the update over the previous public APK in a disposable test environment.
5. For a store-distributed extension, confirm the source loads without a new trust decision. For an ad-hoc installation, expect to trust the new version again and verify that its fingerprint is unchanged.
6. Open existing library entries and chapters before testing newly discovered ones.
7. Exercise popular, latest, search, details, chapter, and media requests.
8. Check pagination, filters, image headers, playback selections, and subtitles as applicable.
9. Verify the APK does not package `entry-source-api`; it must remain `compileOnly`.

Test a clean installation as well as an upgrade. A clean install cannot reveal identity and signing regressions.

## Retire or replace a source

Do not silently reuse an existing source ID for a different provider. If a provider closes, return actionable failures and communicate the retirement through the extension distribution channel. If another provider replaces it, publish it as a distinct source unless it genuinely represents the same stored identities and an explicit migration exists.
