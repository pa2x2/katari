# Readium EPUB evaluation

## Decision

**Conditionally adopted.** Readium Kotlin 3.3.0 is technically viable as the
private engine of Katari's first built-in EPUB processor. The host-side
evaluation proved parsing, model/locator adaptation, reader-host compilation,
and lifecycle cleanup without leaking Readium into Katari's stable BOOK models.

Production adoption remains conditional on app-level dependency, R8, size,
license, and later separately authorized device validation.

## Implemented evaluation

- `:book-api`: Android-free KMP serializable BOOK models.
- `:entry-interactions:book`: built-in processor/SPI prototype and private
  Readium EPUB adapter.
- Readium 3.3.0 `shared`, `streamer`, and `navigator` artifacts only.
- Katari-owned content/publication session boundaries and reverse-order,
  idempotent cleanup.
- Processor-owned `EpubNavigatorFragment` host compile proof.
- Readium-to-Katari publication, navigation, and locator adapters.
- Authored redistribution-safe EPUB 2, EPUB 3 RTL/nested-anchor, and malformed
  fixtures in host tests.

The evaluated module is intentionally not wired into `EntryType`, the app runtime,
source API, persistence, downloads, or production reader UI.

## Proven behavior

- EPUB 2 NCX and EPUB 3 navigation parse into ordered Katari resources.
- Nested navigation and anchors map to stable resource IDs plus locator
  fragments.
- Publication identity/revision come from the Katari content session rather
  than EPUB metadata.
- RTL and language metadata map into Katari-owned models.
- Locators validate progression ranges, finite values, and one-based logical
  positions.
- Locator serialization preserves unknown namespaced extensions and bounded
  text context.
- Same-revision locators restore through the Readium adapter.
- Malformed content returns a structured failure and releases its lease.
- Success, failure, cancellation, repeated close, and close exceptions unwind
  resources in reader/publication/content order.
- No `org.readium` import exists outside `:entry-interactions:book`.

Readium's Android `Uri` use prevents meaningful parsing under plain JVM stubs,
so EPUB parser tests use Robolectric 4.16.1 with JUnit 4/Vintage, isolated to the
BOOK module's test configuration.

## Dependency and packaging evidence

- Current stable Readium version: 3.3.0.
- Katari validation toolchain: wrapper 9.6.1, JDK 21, AGP 9.2.1, Kotlin 2.4.0,
  min SDK 26, compile SDK 37.
- Readium published requirements are compatible on paper: min SDK 23, compile
  SDK 36, Kotlin 2.3.20, Java 11 bytecode.
- Direct Readium AARs total 2,975,665 compressed bytes before R8 and dependency
  overlap.
- Selected AARs contain no native `.so`, manifest permissions/components,
  consumer ProGuard rules, Readium LCP, or DRM binary.
- License: Readium BSD-3-Clause. Embedded PhotoView and bundled font licenses
  need explicit attribution verification because dependency tooling may not
  discover embedded artifacts.

### Media3 condition

`readium-navigator:3.3.0` transitively resolves:

- `androidx.media3:media3-session:1.10.0`;
- `androidx.media3:media3-common-ktx:1.10.0`; and
- `androidx.media3:media3-exoplayer:1.10.0`.

Katari currently catalogs Media3 1.8.0 for anime. The isolated module is not
linked into the app, so it does not yet change the app graph. Production wiring
would likely select 1.10.0 globally and must be validated against anime rather
than excluded blindly.

## Validation passed

```text
./gradlew --quiet spotlessApply
./gradlew --quiet :book-api:testAndroidHostTest
./gradlew --quiet :entry-interactions:book:testDebugUnitTest
./gradlew --quiet :entry-interactions:book:assembleRelease
./gradlew --quiet checkEntryInteractionBoundaries
./gradlew --quiet :entry-interactions:book:dependencyInsight \
  --configuration releaseRuntimeClasspath \
  --dependency androidx.media3:media3-exoplayer
git diff --check
```

Eight focused tests pass: three stable-model tests, two lifecycle tests, and
three EPUB processor/parser tests.

## Conditions before production adoption

1. Wire the BOOK module into a production-shaped app graph and inspect the
   effective dependencies.
2. Validate the Media3 1.10.0 resolution against anime compilation and focused
   playback tests.
3. Run a minified telemetry-enabled release build and address any R8 findings.
4. Compare final APK contribution rather than using raw AAR size alone.
5. Verify dependency-information output and add missing Readium, embedded
   PhotoView, and font attribution.
6. Expand the content-session SPI beyond local materialization to the accepted
   capability-based resource contract.
7. Add security limits for hostile/unbounded archives and more unsupported EPUB
   fixtures before accepting untrusted production content.
8. With separate authorization, verify rendering, gestures, pagination,
   configuration changes, process restoration, and resource loading on a device.

Until these conditions are satisfied, conditional adoption does not constitute
production BOOK support.
