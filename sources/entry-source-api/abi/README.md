# Entry SDK ABI baselines

These encoded JARs are immutable verification inputs for already-built Katari extensions. Each is the `classes.jar` of
an SDK artifact, which is what extensions compile against, built from the change that releases that SDK version. A new
SDK release adds baselines; an existing one is never replaced. A release that Katari stops loading the previous SDK's
extensions in removes that SDK's baselines.

| Baseline | Artifact | SHA-256 after Base64 decoding |
| --- | --- | --- |
| `entry-source-api-sdk-4.0.0.jar.b64` | `com.github.pa2x2.katari:entry-source-api-android:sdk-4.0.0` | `f628ee7b88a6ab6efe377cafb72811fd069617a8ef4a5580f7b7aa669fb152b6` |
| `book-api-sdk-4.0.0.jar.b64` | `com.github.pa2x2.katari:book-api-android:sdk-4.0.0` | `50b16e30ba233bb06e3adb51e719367098fa70a2cc4a9007017aeff4b896bee2` |
| `core-common-sdk-4.0.0.jar.b64` | `com.github.pa2x2.katari:core-common:sdk-4.0.0` | `e79a4ebce2500e2bc169e7593396f2c21971b5c6e0409869640590e055b50802` |

`./gradlew verifyEntrySdkAbi` compares each baseline with Katari's three SDK modules together. From `core-common` it
compares only `eu.kanade.tachiyomi.network`, `eu.kanade.tachiyomi.util` and `tachiyomi.core.common.util.lang`; the rest
of that module is app code the SDK publishes only because `entry-source-api` depends on it.
