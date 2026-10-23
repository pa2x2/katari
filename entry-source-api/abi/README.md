# Released Entry SDK ABI baseline

The encoded JAR is an immutable verification input: extensions built against the last released Entry SDK must keep linking against the current `entry-source-api`.

| Baseline | Release | SHA-256 after Base64 decoding |
| --- | --- | --- |
| `entry-source-api-sdk-2.6.0.jar.b64` | `sdk-2.6.0` (`80f56e19a`) | `02024cee60b8e9ac85d581f27d829d9e64316bc37cbf726d7074712567a60e8f` |

It is `classes.jar` from the JitPack artifact `com.github.pa2x2.katari:entry-source-api-android:sdk-2.6.0`, the AAR extensions built with SDK 2.6.0 compiled against. When a new SDK is released, replace it with that release's `classes.jar`.

Run the check with:

```shell
./gradlew verifyEntrySdkAbi
```
