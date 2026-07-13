# SDK compatibility and versioning

Katari versions the Entry SDK with Semantic Versioning. The SDK artifact version, loader compatibility family, Katari app version, and extension release version are related but distinct.

## Version dimensions

An extension release can involve four values:

| Value | Example | Meaning |
| --- | --- | --- |
| Katari app version | `1.4.0` | App release supplying the SDK runtime implementation |
| SDK artifact | `sdk-2.1.3` | Exact `entry-source-api` artifact used for compilation |
| Extension `versionName` | `2.1.8` | Required API family `2.1`, extension revision `8` |
| Extension `versionCode` | `42` | Android's monotonically increasing update number |

The extension `versionName` is not the SDK SemVer. In this example, `2.1.8` does not mean the extension compiled against SDK `2.1.8`.

## Semantic version rules

SDK releases use `MAJOR.MINOR.PATCH`:

- Increment **PATCH** for backward-compatible bug fixes that add no required public API.
- Increment **MINOR** for backward-compatible public functionality, including new optional capabilities and deprecations.
- Increment **MAJOR** for incompatible public API or behavioral contract changes.

The Git tag and JitPack version use the `sdk-` prefix, such as `sdk-2.1.0`; the semantic version is `2.1.0`.

Examples:

| Change | Release level |
| --- | --- |
| Correct an internal parser helper bug without changing its contract | Patch |
| Add a new optional capability interface | Minor |
| Add a safely defaulted public member after ABI verification | Minor |
| Add a required abstract method to `UnifiedSource` | Major |
| Remove, rename, or move a public class | Major |
| Deprecate a public API while retaining it | Minor |
| Remove a previously deprecated public API | Major |

A hotfix is a patch release, not a fourth numeric component.

## Loader compatibility families

Katari derives the extension's Entry API family from the first two components of its structured `versionName`. The final component is the extension's own revision.

Patch releases stay within one family:

```text
SDK 2.0.0 ─┐
SDK 2.0.1  ├── loader family 2.0
SDK 2.0.2 ─┘
```

A minor SDK release creates another declared family. A Katari runtime supplying SDK 2.1 should continue accepting extensions from family 2.0 because the minor release is backward-compatible, while older Katari releases reject extensions requiring 2.1 APIs.

An extension should declare the oldest family whose public API it actually requires, not automatically copy the newest SDK used by its build.

## Runtime availability

`entry-source-api` is a `compileOnly` dependency. Katari supplies its implementation when loading the extension, so a successful extension build does not prove that an older app contains every referenced symbol.

The SDK changelog identifies the first Katari release supplying each SDK version. For example, a future release record could say:

```text
SDK 2.1.0 → first supplied by Katari 1.4.0
SDK 2.1.1 → first supplied by Katari 1.4.1
```

A patch must not add a new required public symbol. When an extension relies on corrected runtime behavior from a patch, state the minimum Katari app version in the extension release notes or repository metadata.

## Major versions

A major version requires an explicit compatibility decision. Katari can load extensions from an older major only if the app preserves that binary surface through retained classes, a compatibility layer, or a separately namespaced API.

If the new runtime cannot provide the old contract, Katari rejects extensions from the unsupported major and extension authors must migrate. The corresponding changelog entry links to the migration guide.

## Prereleases

Preview artifacts may use tags such as `sdk-2.2.0-alpha.1` or `sdk-3.0.0-rc.1`. They are for coordinated testing, do not become the latest stable documentation, and should not be used by published extensions.

The extension loader family remains a stable numeric `major.minor`; do not put prerelease identifiers into a published extension's compatibility declaration.

## Documentation versions

Human-written guides are organized by compatibility family because patch releases should not change public usage. The generated API reference and SDK changelog identify exact releases.

While only one public SDK family exists, the developer navigation points directly to the current stable guides. When another family is published, older family guides and exact generated references remain available and immutable, while `latest` advances only to a stable release.

See the [SDK changelog](./changelog.md) before adopting a new artifact and [local SDK development](./local-development.md) when testing an unreleased change.
