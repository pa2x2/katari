# Entry SDK ABI baselines

These encoded JARs are immutable verification inputs for already-built Katari extensions. Each is the `classes.jar` of
an SDK artifact, which is what extensions compile against, built from the change that releases that SDK version. A new
SDK release adds baselines; an existing one is never replaced. A release that Katari stops loading the previous SDK's
extensions in removes that SDK's baselines.

| Baseline | Artifact | SHA-256 after Base64 decoding |
| --- | --- | --- |
| `entry-source-api-sdk-4.0.0.jar.b64` | `com.github.pa2x2.katari:entry-source-api-android:sdk-4.0.0` | `4fb4e2059477cb20c82bfc3863d8e56655caaa577cd321493a48bd99025b3ef2` |
| `book-api-sdk-4.0.0.jar.b64` | `com.github.pa2x2.katari:book-api-android:sdk-4.0.0` | `50b16e30ba233bb06e3adb51e719367098fa70a2cc4a9007017aeff4b896bee2` |
| `core-common-sdk-4.0.0.jar.b64` | `com.github.pa2x2.katari:core-common:sdk-4.0.0` | `5069ee4e0aed4653f7217e97e007e917e30d8a43d0fb105d4cbd951c555a9b47` |

`./gradlew verifyEntrySdkAbi` compares each baseline with Katari's three SDK modules together. From `core-common` it
compares only `eu.kanade.tachiyomi.network`, `eu.kanade.tachiyomi.util` and `tachiyomi.core.common.util.lang`; the rest
of that module is app code the SDK publishes only because `entry-source-api` depends on it.
