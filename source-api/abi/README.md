# Supported legacy extension ABI baselines

These encoded JARs are immutable verification inputs for already-built manga extensions. They cover the original upstream Mihon `source-api` runtime ABI and the supported Keiyoushi 1.6 extension surface. They do not cover earlier legacy source APIs published by this fork or the Entry SDK.

| Baseline | Revision | Meaning | SHA-256 after Base64 decoding |
| --- | --- | --- | --- |
| `upstream-mihon-source-api-1.4.jar.b64` | `430b13bb81c8f51331109fa3ff35296f8bde9d27` | Last original-upstream source API state before the TachiyomiX 1.6 API transition | `fcb9fd3b0f246a88e248d5582a9ec88910502a2597586ac36698294344f8634f` |
| `upstream-mihon-source-api-1.6.jar.b64` | `8407439b8a15fd2efc68693605c5e9a639df2494` | Original-upstream `upstream-main` source API used as the 1.6 baseline | `2204a07ed2e89bcee8fac8808269808a00d629880bd8c5b720ad75c7c7901d90` |
| `keiyoushi-extensions-lib-1.6-6e0c96cea8.jar.b64` | `6e0c96cea8` | Keiyoushi extensions-lib 1.6 compile surface used by the first 1.6 repository extensions | `b77f2f2d01ca03a5362eb6477c272f0392ebf5725b62a17ac657b7e1ad9791a2` |

The original Mihon JARs were produced from clean checkouts of the stated commits with JDK 21 and:

```shell
./gradlew --quiet :source-api:assemble
```

The stored file is the Base64 encoding of:

```text
source-api/build/intermediates/aar_main_jar/androidMain/syncAndroidMainLibJars/classes.jar
```

The Keiyoushi JAR is `classes.jar` from the JitPack artifact
`com.github.keiyoushi:extensions-lib:6e0c96cea8` used by extensions-source for its 1.6 compile classpath.

Run all legacy ABI checks with:

```shell
./gradlew --quiet verifyLegacySourceAbi
```
