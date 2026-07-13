# Local SDK development

Use a locally published SDK when a Katari change and an extension change must be tested together before an SDK tag exists. Published extensions must always return to a tagged SDK artifact.

## Prerequisites

- Use the repository's Gradle wrapper.
- Use JDK 21 from `.github/.java-version` for the Katari build.
- Keep `git` available on `PATH`; build logic derives version metadata from it.
- Use an adjacent extension checkout or another extension project that can temporarily resolve Maven Local.

## Publish the local SDK

From the Katari repository root:

```bash
./gradlew --quiet \
    :core:common:publishToMavenLocal \
    :entry-source-api:publishToMavenLocal
```

Both modules use `local-SNAPSHOT` unless `sourceApiVersion` or the `VERSION` environment variable overrides it.

## Consume it from an extension

Temporarily add Maven Local before remote repositories in the extension build and select the local artifact:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}
```

```kotlin
dependencies {
    compileOnly("com.github.katariapp.katari:entry-source-api:local-SNAPSHOT")
}
```

Keep the dependency `compileOnly`. Packaging the SDK into the extension can load duplicate, incompatible classes instead of Katari's runtime classes.

Build the extension's debug APK, then test it against a Katari build containing the matching runtime change. Exercise the complete source lifecycle and the specific new capability or content-type behavior.

Before publishing an extension, remove `mavenLocal()`, replace `local-SNAPSHOT` with a stable `sdk-*` tag, rebuild, and confirm the APK does not package SDK classes.
