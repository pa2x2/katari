# Supported Aniyomi extension ABI baselines

These encoded JARs are immutable verification inputs for already-built Aniyomi extensions. Each is the `classes.jar`
of an extensions-lib stub AAR that extensions in use were compiled against. A new library release or Komikku update adds
a baseline; an existing one is never replaced.

| Baseline | Artifact | Revision | SHA-256 after Base64 decoding |
| --- | --- | --- | --- |
| `aniyomi-extensions-lib-14.jar.b64` | `com.github.aniyomiorg:extensions-lib:14` | `f33cc24` | `44b9dabf5bd14377a48a73e8ce3da0223e829407098e09547a174770608c9e49` |
| `aniyomi-extensions-lib-16.jar.b64` | `com.github.aniyomiorg:extensions-lib:v16` | `a776b99` | `cf981398207aaf4249784ed3772e426dd0a9de27f3ca83d6851e6f45a8a047c0` |
| `aniyomi-extensions-lib-17.jar.b64` | `com.github.aniyomiorg:extensions-lib:v17` | `971ce96` | `7b4f6d648f556b337ab7b8a7c0e2ea22b64f693c40a9e395e701f42323c6d86a` |
| `komikku-aniyomi-extensions-lib-f5961b5dfb.jar.b64` | `com.github.komikku-app:aniyomi-extensions-lib:f5961b5dfb` | `f5961b5dfb` | `85913c74fa02583a6e2cfae28cf62d0bd1fd408eb32a412ef426d5f12ec726ac` |

All four come from JitPack. Komikku's fork is what Yūzōnō's extensions compile against.

`./gradlew verifyAniyomiSourceAbi` compares each baseline's `animesource`, `torrentutils`, `network`, `util` and
`AppInfo` classes with Katari's. The root `build.gradle.kts` lists, with a reason for each, the members no extension
can link against and the japicmp change types that don't break dex extensions.
