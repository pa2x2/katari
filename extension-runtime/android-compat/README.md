# Android stand-ins for the desktop extension host

Extensions are Android code. On desktop they run against the classes in this module, on top of a stub of the
Android SDK's `android.jar` that the build makes from the installed SDK (`AndroidStubJarTask`; it is not committed).
Classes this module defines are taken out of the stub, so the stub covers only what nothing here implements, and
each of its methods throws `"Stub!"`.

Only the desktop host uses this module. Nothing on Android depends on it.

## Where the code comes from

Most of `src/main/java` is copied from Suwayomi-Server at commit `6b481e8` (2026-10-21), directory
`AndroidCompat/src/main/java`, with every file's header and package kept. Their licences are in `LICENSE`:

- Files with an Android Open Source Project or Andy Bao header are under the Apache License 2.0.
- Files with a Suwayomi header, and files with no header, are under the Mozilla Public License 2.0, Suwayomi's
  licence. Two headerless exceptions: `android/os/shadows/` is derived from Robolectric (MIT), and
  `kotlinx/coroutines/android/HandlerDispatcher.kt` from kotlinx.coroutines (Apache License 2.0).

### Copied

| Tree | Files |
|---|---|
| `android/**` (annotation, app, arch, content, database incl. `sqlite`, graphics, net, os, preference, support, text, util, view, widget) | 231 |
| `androidx/core` | 1 |
| `com/android/internal` | 5 |
| `dalvik` | 3 |
| `io/requery` | 1 |
| `kotlinx/coroutines/android` | 1 |
| `libcore` | 3 |
| `org/json` | 7 |
| `rx/android/schedulers` (Suwayomi keeps it in a directory named `rx.android.schedulers`) | 1 |
| `xyz/nulldev/androidcompat`: `replace/`, `res/`, `info/`, `io/AndroidFiles`, `util/SafePath`, `util/UriExtensions`, and what the copied Android classes call: `androidimpl/` (the context and package manager), `db/ScrollableResultSet`, `service/ServiceSupport` | 17 |

`android/database/sqlite` is copied without a JDBC driver; none is added until an extension needs a database.

### Held back

- `app/cash/quickjs/**` and `com/squareup/duktape/**`, which run on GraalJS. Katari's own QuickJS bridge in
  `source-compat` is the only `QuickJs`, and Android has no Duktape.
- `android/webkit/**`, `org/chromium/**`, `xyz/nulldev/androidcompat/webkit/**` and `CallableArgument`, the web view on
  JCEF. Until desktop has a web view, the stub's `WebView` throws.
- Suwayomi's server glue: `AndroidCompat`, `AndroidCompatModule`, `AndroidCompatInitializer`, `util/KoinGlobalHelper`,
  `pm/` (`PackageController`, `InstalledPackage`, `PackageUtil`), `config/` and `io/sharedprefs/`. Katari's host
  replaces them.
- The Noto fonts in Suwayomi's resources. `Typeface` falls back to the JDK's fonts without them.

### Replaced

- Suwayomi's nine minimal `androidx.preference` classes. `src/main/java/androidx/preference` holds Katari's model of
  AndroidX Preference 1.2.1 instead (Apache License 2.0): `Preference`, `PreferenceGroup`, `PreferenceScreen`,
  `PreferenceCategory`, `PreferenceManager`, `PreferenceDataStore`, `TwoStatePreference`, `SwitchPreferenceCompat`,
  `CheckBoxPreference`, `DialogPreference`, `ListPreference`, `MultiSelectListPreference`, `EditTextPreference` and
  `SeekBarPreference`. They keep AndroidX's keys, titles, summaries and summary providers, values, entries,
  listeners, dependencies, attach and detach, and persistence through `SharedPreferences` or a
  `PreferenceDataStore`. They have no views, XML inflation, styled attributes or saved instance state.
- Koin and Suwayomi's config: the copied classes read what the host provides from
  `mihon.extension.compat.AndroidCompatEnvironment` (`src/main/kotlin`).
- SLF4J and kotlin-logging: `android.util.Log` writes to the host's logcat loggers through
  `mihon.extension.compat.LogcatLog`, and the other copied classes log through `android.util.Log`.

### Changed in copied files

Katari changed these copied files; everything else is as Suwayomi has it.

- Koin and config lookups now read `AndroidCompatEnvironment`: `android/app/Service.java`,
  `android/os/Environment.java`, `xyz/nulldev/androidcompat/androidimpl/CustomContext.java`,
  `androidimpl/FakePackageManager.java` (it knows only the host's own package), `info/ApplicationInfoImpl.kt`,
  `io/AndroidFiles.kt` (the folders are laid out under the host's data and cache folders) and
  `res/BuildConfigCompat.java`.
- `android/os/Build.java`: `IS_DEBUGGABLE` is false, as on a user's phone; `BuildConfigCompat.DEBUG` and
  `ApplicationInfoImpl.debug` likewise.
- `android/os/SystemProperties.java` answers every read as Android does for an unset property.
- `dalvik/system/BaseDexClassLoader.java` loads jars only; the host converts extension APKs to jars itself.
- `android/widget/Toast.java` hands its text to the host.
- `android/os/Looper.java`: `prepareMainLooper`, which Android deprecates because the environment creates the main
  looper, delegates to `prepareEnvironmentMainLooper`, which the host, being that environment, calls.
- Logging through `android.util.Log` instead of SLF4J or kotlin-logging: `android/util/Log.java`,
  `android/support/multidex/MultiDex.java`, `dalvik/system/CloseGuard.java`, `service/ServiceSupport.kt`.
- Java 21 constructs the build's Java 17 target lacks: `android/util/LruCache.java`, `android/graphics/Canvas.java`.
- `@Deprecated` on members whose Javadoc deprecates them (`android/content/Intent.java`,
  `android/content/pm/PackageManager.java`, `android/net/ConnectivityManager.java`, `android/net/NetworkInfo.java`)
  and on Kotlin overrides of deprecated JDBC methods (`db/ScrollableResultSet.kt`).
