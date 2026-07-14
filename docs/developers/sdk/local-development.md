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
./gradlew --quiet publishEntrySdkToMavenLocal
```

The aggregate task publishes `core-common`, `book-api`, and `entry-source-api` with the same version. All use `local-SNAPSHOT` unless `sourceApiVersion` or the `VERSION` environment variable overrides it. Use the aggregate task even when a change appears to touch only one artifact; this prevents locally inconsistent transitive metadata.

## Consume it from an extension

The public `katari-extensions` build has one local-development switch. From that repository, add `-PuseMavenLocal=true` to the normal module task:

```bash
./gradlew --quiet \
    -PuseMavenLocal=true \
    :src:en:example:assembleDebug
```

The switch both enables Maven Local and selects `entry-source-api:local-SNAPSHOT`; do not edit `gradle.properties`, `settings.gradle.kts`, or a module dependency. Without the switch, the build resolves the tagged SDK configured in `gradle.properties`.

Another extension project can adopt the same convention by conditionally adding Maven Local:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        if (gradle.startParameter.projectProperties["useMavenLocal"]?.toBoolean() == true) {
            mavenLocal()
        }
        maven(url = "https://www.jitpack.io")
    }
}
```

and resolving the version from that same switch:

```kotlin
val useMavenLocal = providers.gradleProperty("useMavenLocal").orNull?.toBoolean() == true
val katariSdkVersion = if (useMavenLocal) {
    "local-SNAPSHOT"
} else {
    providers.gradleProperty("katariSourceApiVersion").get()
}

dependencies {
    compileOnly("com.github.katariapp.katari:entry-source-api:$katariSdkVersion")
}
```

::: warning

Keep the dependency `compileOnly`. Packaging the SDK into the extension can load duplicate, incompatible classes instead of Katari's runtime classes.

:::

Build the extension's debug APK, then test it against a Katari build containing the matching runtime change. Exercise the complete source lifecycle and the specific new capability or content-type behavior.

::: warning

Before publishing an extension, build without `-PuseMavenLocal`, confirm that the configured version is a stable `sdk-*` tag, and verify that the APK does not package SDK classes. The tagged SDK may expose `book-api` transitively; it must not be packaged either.

:::