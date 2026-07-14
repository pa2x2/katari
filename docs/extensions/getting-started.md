# Create your first extension

This guide covers the pieces that make an Android APK discoverable as a Katari extension.

## Add the SDK

Add JitPack to dependency resolution in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}
```

Add the Entry SDK as a compile-only dependency in the extension module:

```kotlin
val katariSdkTag = "sdk-2.1.0"

dependencies {
    compileOnly("com.github.katariapp.katari:entry-source-api:$katariSdkTag")
}
```

`sdk-2.1.0` is the current stable Entry SDK. Check [Katari tags](https://github.com/katariapp/katari/tags) and the [SDK changelog](../developers/sdk/changelog.md) before adopting a later release.

`compileOnly` is intentional. Katari supplies the API and its runtime dependencies when it loads the extension; packaging another copy in the APK can cause incompatible classes to be loaded.

Book sources use `BookContentDescriptor` and related models from the transitive `book-api` artifact. Do not add a separately versioned `book-api` dependency; both public artifacts are published together under the selected SDK tag.

The current SDK requires Android API 26 or newer. A typical extension module uses the following Android configuration:

```kotlin
val entryApiFamily = "2.1"

android {
    namespace = "eu.kanade.tachiyomi.extension.all.example"
    compileSdk = 37

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.extension.all.example"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "$entryApiFamily.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

Use a stable, unique application ID. Changing it later makes Android and Katari treat the extension as a different installation.

## Declare the extension

Add the extension feature and metadata to the module's `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="tachiyomi.extension" />

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Katari: Example">

        <meta-data
            android:name="tachiyomi.extension.class"
            android:value=".ExampleFactory" />

        <meta-data
            android:name="tachiyomi.extension.nsfw"
            android:value="0" />
    </application>

</manifest>
```

The class name is resolved relative to the extension's application ID when it begins with a dot. Set `tachiyomi.extension.nsfw` to `1` when the extension exposes adult content.

Set `entryApiFamily` to the major and minor family declared by the selected SDK release. Katari reads that family from the first two components of `versionName`; the final component is the extension revision. Increase the final component for extension releases and keep `versionCode` monotonically increasing.

SDK `2.1` is the first family containing BOOK. An extension that imports BOOK symbols must use a `2.1.x` extension `versionName` and requires a Katari release that supplies that family. It must not advertise `2.0.x` compatibility merely because the rest of its source lifecycle is unchanged.

## Create a factory

The manifest points to a public class with a no-argument constructor. That class may implement `UnifiedSource` directly or implement `EntrySourceFactory`. A factory can return one source or several:

```kotlin
package eu.kanade.tachiyomi.extension.all.example

import eu.kanade.tachiyomi.source.entry.EntrySourceFactory
import eu.kanade.tachiyomi.source.entry.UnifiedSource

class ExampleFactory : EntrySourceFactory {
    override fun createSources(): List<UnifiedSource> = listOf(ExampleSource())
}
```

Keep the factory small. Request construction, parsing, filters, preferences, and media resolution belong in the source or in focused supporting files.

For an extension with one source, the manifest may point directly to the public `UnifiedSource` implementation instead.

## Create a source

This abbreviated image-source skeleton shows the required lifecycle:

```kotlin
package eu.kanade.tachiyomi.extension.all.example

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryImageHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceMetadata

internal class ExampleSource : EntryImageHttpSource(), SourceMetadata {
    override val name = "Example"
    override val lang = "en"
    override val baseUrl = "https://example.com"
    override val supportsLatest = true
    override val supportedEntryTypes = setOf(EntryType.MANGA)

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
        error("Request and parse the popular catalogue")

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> =
        error("Request and parse the latest catalogue")

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = error("Request and parse search results")

    override suspend fun getContentDetails(entry: SEntry): SEntry =
        entry.copy().apply {
            initialized = true
            type = EntryType.MANGA
        }

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> =
        error("Request and parse chapters")

    override suspend fun getMedia(
        chapter: SEntryChapter,
        selection: PlaybackSelection,
    ): EntryMedia = error("Return EntryMedia.ImagePages")
}
```

`supportedEntryTypes` lets Katari describe the source before loading its catalogue. Catalogue entries must still set `type` as soon as it is known; do not wait for the details request if the listing already provides enough information.

Continue with the [Entry SDK overview](../developers/sdk/README.md) and [data model](../developers/sdk/data-model.md) for concrete entry and media payloads.

## Test against a local Katari checkout

For coordinated app and extension changes, follow [local SDK development](../developers/sdk/local-development.md). It covers publishing `local-SNAPSHOT`, testing an extension against it, and returning to a tagged SDK before publication.
