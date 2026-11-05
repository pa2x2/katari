# Desktop Changelog

## [0.1.0-alpha.3] - 2026-11-05

### ✨ Added

- Manga chapters open in the app's reader.
- Right-clicking a page offers copying it, saving it and setting it as the cover.
- Manga show in the immersive feed, and series screens show page previews.

### 🔄 Changed

- The library's refresh button sits in the app bar and updates the category shown, and Update entire library is in the menu.

### 🧩 Improved

- Screens you can pull to refresh also have a refresh button.

### 🐛 Fixed

- Scrolling past the top of a list with the wheel no longer leaves the refresh indicator stuck.
- The immersive view no longer crashes when a refresh returns fewer titles.

## [0.1.0-alpha.2] - 2026-11-05

### ✨ Added

- Anime episodes play in the app through mpv.
- Episodes can keep playing in a picture-in-picture window.
- The desktop's media controls show the playing episode and can pause it, change episodes and raise the window.
- Manga chapters, episodes and books can be downloaded.

### 🐛 Fixed

- The app no longer crashes after a library update or another task that waits for a connection.
- The window opens at its saved size under window managers that frame it differently.
- Moving to another screen no longer takes the window out of full screen.
- Leaving full screen from a maximized window works.
- Chapter and episode rows no longer grow taller under the pointer.
- Links with characters such as `|` in them open in the browser.
- About labels pre-release builds as pre-releases instead of stable.

## [0.1.0-alpha.1] - 2026-11-03

### ✨ Added

- Mihon and Entry SDK extensions, Aniyomi extensions and LNReader plugins install from the same stores as on Android.
- The library, updates, history, upcoming, statistics, migration, profiles and backups work as on Android.
- Cloudflare checks and extension logins open in a built-in web view.
- The AppImage checks for new desktop releases and updates itself.

[Unreleased]: https://github.com/pa2x2/katari/compare/desktop-v0.1.0-alpha.3...HEAD
[0.1.0-alpha.3]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.3
[0.1.0-alpha.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.1
