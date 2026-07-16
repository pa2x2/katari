# SDK compatibility and versioning

Katari versions the Entry SDK with Semantic Versioning. The SDK artifact version, loader compatibility family, Katari app version, and extension release version are related but distinct.

## Version dimensions

An extension release can involve four values:

| Value                   | Example     | Meaning                                                                          |
| ----------------------- | ----------- | -------------------------------------------------------------------------------- |
| Katari app version      | `1.2.2`     | App release supplying the SDK runtime implementation                             |
| SDK artifact set        | `sdk-2.2.0` | Exact coordinated `entry-source-api` and `book-api` version used for compilation |
| Extension `versionName` | `2.2.8`     | Required API family `2.2`, extension revision `8`                                |
| Extension `versionCode` | `42`        | Android's monotonically increasing update number                                 |

The extension `versionName` is not the SDK SemVer. In this example, `2.2.8` does not mean the extension compiled against SDK `2.2.8`.

## Semantic version rules

SDK releases use `MAJOR.MINOR.PATCH`:

- Increment **PATCH** for backward-compatible bug fixes that add no required public API.
- Increment **MINOR** for backward-compatible public functionality, including new optional capabilities and deprecations.
- Increment **MAJOR** for incompatible public API or behavioral contract changes.

The Git tag and JitPack version use the `sdk-` prefix, such as `sdk-2.2.0`; the semantic version is `2.2.0`.

Examples:

| Change                                                              | Release level |
| ------------------------------------------------------------------- | ------------- |
| Correct an internal parser helper bug without changing its contract | Patch         |
| Add a new optional capability interface                             | Minor         |
| Add a new supported `EntryType` and media payload                   | Minor         |
| Add a safely defaulted public member after ABI verification         | Minor         |
| Add a required abstract method to `UnifiedSource`                   | Major         |
| Remove, rename, or move a public class                              | Major         |
| Deprecate a public API while retaining it                           | Minor         |
| Remove a previously deprecated public API                           | Major         |

A hotfix is a patch release, not a fourth numeric component.

## Loader compatibility families

Katari derives the extension's Entry API family from the first two components of its structured `versionName`. The final component is the extension's own revision.

Patch releases stay within one family:

```text
SDK 2.0.0 ─┐
SDK 2.0.1  ├── loader family 2.0
SDK 2.0.2 ─┘
```

A minor SDK release creates another declared family. The Katari runtime supplying SDK 2.2 continues accepting extensions from families 2.0 and 2.1 because the minor releases are backward-compatible, while older Katari releases reject extensions requiring 2.2 APIs.

An extension should declare the oldest family whose public API it actually requires, not automatically copy the newest SDK used by its build.

Adding a supported content type can add values to public enums and implementations to sealed media contracts. This is binary-compatible for already compiled extensions but can make an exhaustive Kotlin `when` fail when the extension is recompiled. Extension code that handles types or media generically must include an `else` branch that reports or ignores an unsupported future value rather than assuming the current list is permanently closed.

## Runtime availability

The Entry SDK is a `compileOnly` dependency. Katari supplies `entry-source-api` and its transitive `book-api` contracts when loading the extension, so a successful extension build does not prove that an older app contains every referenced symbol.

The SDK changelog identifies the public contracts added by each SDK version. Runtime support and artifact publication are coordinated so an extension declaring a supported family can rely on Katari supplying that family.

`sdk-2.2.0`, first supplied by Katari `1.2.2`, introduces `RelatedEntriesSource`. Katari accepts the `2.0`, `2.1`, and `2.2` Entry SDK families.

`sdk-2.1.0`, first supplied by Katari `1.2.0`, introduced BOOK and the coordinated `book-api` artifact.

A patch must not add a new required public symbol. When an extension relies on corrected runtime behavior from a patch, state the minimum Katari app version in the extension release notes or repository metadata.

## Major versions

A major version requires an explicit compatibility decision. Katari can load extensions from an older major only if the app preserves that binary surface through retained classes, a compatibility layer, or a separately namespaced API.

If the new runtime cannot provide the old contract, Katari rejects extensions from the unsupported major and extension authors must migrate. The corresponding changelog entry links to the migration guide.

## Prereleases

Preview artifacts may use tags such as `sdk-2.2.0-alpha.1` or `sdk-3.0.0-rc.1`. They are for coordinated testing, do not become the latest stable documentation, and should not be used by published extensions.

The extension loader family remains a stable numeric `major.minor`; do not put prerelease identifiers into a published extension's compatibility declaration.

## Documentation versions

The documentation site publishes one set of human-written guides and generated API references for the current stable SDK. These pages advance when another stable SDK becomes current; the site does not retain separate guide or API-reference archives for older families or patch releases.

Use the SDK changelog to identify when a public contract was introduced or changed. For an older exact release, use the corresponding `sdk-*` tag as the source-level reference.

See the [SDK changelog](./changelog.md) before adopting a new artifact and [local SDK development](./local-development.md) when testing coordinated local changes.
