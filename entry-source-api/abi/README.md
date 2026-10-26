# Entry SDK ABI baselines

These encoded JARs are immutable verification inputs for already-built Katari extensions. Each is the `classes.jar` of
an SDK artifact as JitPack published it, which is what extensions compile against. A new SDK release adds baselines; an
existing one is never replaced.

| Baseline | Artifact | Revision | SHA-256 after Base64 decoding |
| --- | --- | --- | --- |
| `entry-source-api-sdk-3.0.0.jar.b64` | `com.github.pa2x2.katari:entry-source-api-android:sdk-3.0.0` | `ec49411c3` | `c564e9494978a0fae1183421d199f1408025bbfaf855c8980e32cc7ab4aa33c3` |
| `book-api-sdk-3.0.0.jar.b64` | `com.github.pa2x2.katari:book-api-android:sdk-3.0.0` | `ec49411c3` | `a71b395f6cb24d6d901a9350c2cf4e8ceb0f1597f823621f57d45df7c0af8dba` |
| `core-common-sdk-3.0.0.jar.b64` | `com.github.pa2x2.katari:core-common:sdk-3.0.0` | `ec49411c3` | `f3ebde6363a72e63a8269554c579142f7626667a7a523ac5eb248f0610d09d58` |

`./gradlew verifyEntrySdkAbi` compares each baseline with Katari's three SDK modules together. From `core-common` it
compares only `eu.kanade.tachiyomi.network`, `eu.kanade.tachiyomi.util` and `tachiyomi.core.common.util.lang`; the rest
of that module is app code the SDK publishes only because `entry-source-api` depends on it.
