# Desktop Changelog

## [1.0.0-beta.2] - 2026-11-06

### 🧩 Improved

- On wide windows, entry covers in the library, browse, history and statistics grow, so the window shows bigger covers rather than more small ones.
- On wide windows, the statistics reading calendar's days grow to fill its card instead of leaving empty space on the right.

### 🔄 Changed

- On wide windows, the statistics dashboard stays at two columns of cards, which widen to fill the width, so a section with two or three cards no longer leaves an empty column.

## [1.0.0-beta.1] - 2026-11-06

### 🔄 Changed

- On wide windows, screens hold their content in a centred frame. The title, tabs, list and main button share one edge, and a wider window adds columns or panes rather than longer rows.

### 🧩 Improved

- On wide windows, the sources and extensions lists wrap their rows into columns.
- On wide windows, global search and the statistics dashboard show more cards in columns, and the dashboard's headline figures sit in one line when there is room.

## [0.1.0-alpha.4] - 2026-11-05

### ✨ Added

- Books open in the desktop app's reader.

### 🐛 Fixed

- On X11 and Xwayland, a touchpad swipe turns one page in the manga reader instead of several.
- On X11 and Xwayland, a touchpad swipe moves one tab in the library and other tabbed screens instead of several.
- Page numbers in two-page spreads no longer jump on wide windows.

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

[Unreleased]: https://github.com/pa2x2/katari/compare/desktop-v1.0.0-beta.2...HEAD
[1.0.0-beta.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.2
[1.0.0-beta.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.1
[0.1.0-alpha.4]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.4
[0.1.0-alpha.3]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.3
[0.1.0-alpha.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.1
