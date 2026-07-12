# Precompiled original-upstream 1.4 fixture

`legacy14-fixture.jar.b64` is a Base64-encoded JAR compiled from `Legacy14Fixture.java` against the original upstream Mihon 1.4 baseline at commit `430b13bb81c8f51331109fa3ff35296f8bde9d27`.

The decoded JAR SHA-256 is:

```text
d10f2a3729b26ecea44ac19c815199a87978bef5698af866f700418e1f5b5963
```

The fixture deliberately calls the historical `Source$DefaultImpls.getMangaDetails` and `CatalogueSource$DefaultImpls.getChapterList` bytecode owners. Tests run that already-compiled bytecode against the current app runtime; the fixture must not be recompiled as part of the normal build.
