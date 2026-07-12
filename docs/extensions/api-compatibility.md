# API compatibility and versioning

Katari encodes an extension's API family and its own revision in the APK `versionName`. The SDK artifact tag is a separate value.

| Value | Example | Meaning |
| --- | --- | --- |
| APK `versionName` | `<major>.<minor>.3` | Selected Entry API family, extension revision `3` |
| APK `versionCode` | `13` | Android's monotonically increasing update number |
| SDK dependency | `sdk-<release>` | Exact published `entry-source-api` artifact used to compile |

The SDK tag and the extension revision are independent. An extension can increase the final component of `versionName` while continuing to compile against the same SDK artifact.

## Select an SDK

Use a tagged SDK in published builds:

```kotlin
val katariSdkTag = "<latest sdk-* tag>"

dependencies {
    compileOnly("com.github.katariapp.katari:entry-source-api:$katariSdkTag")
}
```

Select the latest compatible tag from [Katari releases](https://github.com/katariapp/katari/tags).

Use `local-SNAPSHOT` only while testing coordinated changes against a local Katari checkout. The dependency remains `compileOnly` because Katari supplies the API and runtime implementation. Bundling the SDK in an extension can create duplicate, incompatible classes.

An SDK patch tag can add fixes or compatible APIs without changing the loader family. Compiling successfully does not by itself prove that every Katari release in the family contains a newly added API. If an extension uses a symbol introduced by a newer SDK, it also requires a Katari version that supplies that symbol at runtime.

## Encode the loader family in `versionName`

Encode the API family declared by the selected SDK in the first two components and use the last component as that extension's revision:

```kotlin
val entryApiFamily = "<API major>.<API minor>"

android {
    defaultConfig {
        versionCode = 4
        versionName = "$entryApiFamily.4"
    }
}
```

For an installed APK without separate library metadata, Katari removes the final component and validates the resulting API family before loading extension classes. The Katari extension repository derives `extensionLib` the same way.

The APK loader can read explicit `tachiyomix.extensionLib` metadata for compatibility with other packaging schemes, but Katari's extension authoring and repository workflow expects the structured `versionName`. Do not use explicit metadata to give a Katari extension an unrelated version scheme.

The loader accepts the Entry API families supported by that Katari release. It also has separate compatibility paths for selected legacy Mihon API families; those paths do not make legacy `source-api` the authoring API for new extensions.

## Compatible and incompatible changes

Within a compatibility family, extension authors should still treat these changes cautiously:

- Calling a newly added API requires an app release that implements it.
- Removing or changing a public method, model field, constructor, or serialized meaning can require a new family.
- Moving public classes between packages is a binary compatibility break.
- Changing either of the first two `versionName` components changes the declared API family.
- Changing source IDs or content URLs is a data-identity change even when binary compatibility is unaffected.
- Changing Android minimum API requirements can exclude devices independently of Entry API compatibility.

Adding an optional capability interface is useful only when the installed Katari version recognizes it. Provide a reasonable baseline behavior when the core `UnifiedSource` contract permits one.

## Upgrade workflow

When adopting a newer SDK:

1. Read the SDK release notes and inspect the public contracts your extension uses.
2. Update the tagged `compileOnly` dependency.
3. Keep the first two components of `versionName` unchanged unless the SDK release explicitly introduces another family.
4. Compile and test against the oldest Katari release you intend to support.
5. Test against the current Katari release.
6. Increase the final component of `versionName`, publish with a higher APK `versionCode`, and preserve the package and signing identity.

If the extension must use a newly introduced symbol, state the minimum Katari version in its release notes or repository metadata. Do not claim support for older app releases based only on a shared family label.

## Local SDK development

For a coordinated API change, publish the local modules:

```bash
./gradlew --quiet :core:common:publishToMavenLocal :entry-source-api:publishToMavenLocal
```

Then add `mavenLocal()` to the extension build and use `local-SNAPSHOT`. Before publishing the extension, return to a tagged SDK and verify the build again. See [publishing and maintenance](./publishing.md) for the complete release checklist.
