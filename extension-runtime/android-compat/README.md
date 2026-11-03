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
| `android/**` (annotation, app, arch, content, database incl. `sqlite`, graphics, net, os, preference, support, text, util, view, widget, and `webkit`'s `URLUtil` and `MimeTypeMap`) | 232 |
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
- The rest of `android/webkit/**`, `org/chromium/**`, `xyz/nulldev/androidcompat/webkit/**` and `CallableArgument`,
  Suwayomi's web view on KCEF. `URLUtil` and `MimeTypeMap` are copied: they only read strings, and extensions call them
  without a web view. `CookieManager` and the web view are Katari's (below).
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
- Suwayomi's `android/widget/EditText`, which only threw. `android/widget/TextView` and `EditText` hold Katari's model
  of AOSP's (Apache License 2.0): the text, hint, input type with its single-line rule, IME options, lines, horizontal
  scrolling, error, filters, watchers and selection that a source's text setting configures its editor with, so the
  settings host can run the source's bind listener and read the result. They have no layout, drawing, resources, spans
  or input handling.

### Added

Katari's models of classes Suwayomi leaves to the stub, each derived from AOSP's or AndroidX's (Apache License 2.0)
and limited to what extensions use:

- `android/content/res/Resources` and `Configuration`, and `android/util/DisplayMetrics`: the configuration and display
  metrics a context's and the system's resources report. The UI mode is normal and light or dark as the desktop
  prefers (`AndroidCompatEnvironment.isNightMode`), the density AOSP's default. There are no resource tables, so every
  lookup by id throws `NotFoundException`, and themes resolve no styles.
- `android/graphics/ColorMatrix`, `ColorFilter` and `ColorMatrixColorFilter`, which `Canvas` applies when it draws a
  bitmap.
- `androidx/core/content/ContextCompat`, which Katari's Android app ships, so extensions call it without bundling it:
  `getMainExecutor`.
- `android/webkit/CookieManager`: AOSP's class with an implementation after Android's WebView one, over the app's own
  cookie store, which the host provides as `mihon.extension.compat.WebViewCookies`; so the cookies an extension sets are
  the ones the app's HTTP clients and web view send. URLs are normalised by `WebAddress`, an unparsable URL or cookie
  and a `Secure` cookie for a URL that isn't HTTPS are ignored, and callbacks run on the calling thread's `Looper`,
  which a call with a callback needs. AOSP's hidden `getCookie` and `hasCookies` overloads are left out. File scheme
  cookies are never kept.
- `android/webkit/WebView`, `WebSettings`, `WebViewClient`, `WebChromeClient`, `WebResourceResponse`,
  `WebResourceError`, `ConsoleMessage` and `RenderProcessGoneDetail`, from AOSP's (each file says what changed): a web
  view runs on one of the host's hidden pages, which the host provides as `mihon.core.common.web.ExtensionPages` and
  `mihon.extension.compat.webkit.WebViewPage` drives. It loads URLs with their headers and documents at a base URL,
  runs scripts and hands back their values, gives its pages the objects of `addJavascriptInterface` and passes their
  calls on, asks its client to answer requests, and tells its clients of loads, titles, errors, console messages and its
  renderer's end, on the main looper's thread as Android does; a visual state callback comes once the page has taken
  the calls before it, as the page is drawn only while a dialog shows it. Its settings' user agent, scripts and images
  reach the page; the rest are kept and read back. A view an extension forgets closes its page once collected. What a
  page nobody sees can't do, such as history, zoom, find or printing, throws as the stub does, and without the host's
  pages so does the constructor, which reports the source as needing a web view.
- `android/app/Activity`, `ActivityThread` and `Dialog`, and `android/view/ContextThemeWrapper`, which `Activity`
  extends: the foreground activity extensions look for to show a dialog over, and the dialog. The host launches one
  activity for the app's window as it opens and destroys it as it closes; its lifecycle reaches the application's
  lifecycle callbacks, and `ActivityThread` (a hidden class) answers the reflection extensions use to find an activity
  resumed before they registered those callbacks. A dialog over a running activity whose content is a `WebView` opens in
  the app's window, which the host provides as `mihon.extension.compat.ExtensionDialogs`, showing that web view's page;
  any other content throws as the dialog is shown. The dialog has no `Window` (`getWindow()` is null, as the SDK
  allows), so what an extension sets on one is never asked for. The user closing it is a back press, which cancels a
  cancelable dialog; its listeners run on the thread it was made on.
- Koin and Suwayomi's config: the copied classes read what the host provides from
  `mihon.extension.compat.AndroidCompatEnvironment` (`src/main/kotlin`). Installing it also sets the system properties
  Android's runtime sets for an app (`http.agent`, Dalvik's user agent).
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
- `android/os/Build.java`'s device strings are what Android reads from those unset properties, `"unknown"` or empty,
  instead of null, and its ABI lists are empty. Its version is Android 7.0 (API 24), the release the copied `Context`
  comes from, so extensions take the code paths the stand-ins implement; `VERSION_CODES` lists every release up to 36,
  with Suwayomi's wrong value of `O` corrected.
- `xyz/nulldev/androidcompat/androidimpl/CustomContext.java` returns the host's `Resources` instead of null.
- `android/graphics/Canvas.java` applies a paint's `ColorMatrixColorFilter` to the bitmaps it draws.
- `android/view/View.java`: `getParent` returns the view's parent, as AOSP's does, instead of throwing; no view is ever
  added to another here, so it is null.
- `dalvik/system/BaseDexClassLoader.java` loads jars only; the host converts extension APKs to jars itself.
- `android/widget/Toast.java` hands its text to the host.
- `android/os/Looper.java`: `prepareMainLooper`, which Android deprecates because the environment creates the main
  looper, delegates to `prepareEnvironmentMainLooper`, which the host, being that environment, calls.
- Logging through `android.util.Log` instead of SLF4J or kotlin-logging: `android/util/Log.java`,
  `android/support/multidex/MultiDex.java`, `dalvik/system/CloseGuard.java`, `service/ServiceSupport.kt`.
- `android/graphics/BitmapFactory.java` returns null for data it can't decode, as Android does, instead of throwing.
- Java 21 constructs the build's Java 17 target lacks: `android/util/LruCache.java`, `android/graphics/Canvas.java`.
- `@Deprecated` on members whose Javadoc deprecates them (`android/content/Intent.java`,
  `android/content/pm/PackageManager.java`, `android/net/ConnectivityManager.java`, `android/net/NetworkInfo.java`)
  and on Kotlin overrides of deprecated JDBC methods (`db/ScrollableResultSet.kt`).
