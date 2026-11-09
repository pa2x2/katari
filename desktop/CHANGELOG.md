# Desktop Changelog

## [1.0.0-beta.5] - 2026-11-09

### ✨ Added

- The manga reader recognises text on pages to select, copy, translate and read aloud, with keyboard shortcuts.
- Downloaded manga chapters can be translated in the background.
- Each device chooses what it syncs: history, statistics, covers, fonts and other data, settings by area, and which profiles.
- Syncing can be paused, run as often as you choose, and keep changes off metered networks.
- Settings rows show whether each setting syncs, and each Settings screen opens its sync choices.

### 🧩 Improved

- The sync status warns when synced changes wait for a Katari update.

### ⚡️ Performance

- The app starts faster.

### 🐛 Fixed

- Windows open at their final size and layout instead of resizing after they appear.
- Chapters a library update finds on one device now reach your other devices.
- A device's first library update no longer fills Updates with a title's old chapters.
- Opening the Sync page from a notification while it is already open no longer crashes the app.

## [1.0.0-beta.4] - 2026-11-08

### 🧩 Improved

- The sync link and invite code fields take a code typed or pasted in any form and show it in dashed groups of four.

### 🐛 Fixed

- Syncing no longer hides statistics a device recorded before another device started counting, and statistics hidden this way show again.
- The offer to go where another device got further shows the place on its own line, so its name no longer breaks halfway.
- Episodes whose audio comes as a separate track no longer play without sound for their first moments.

## [1.0.0-beta.3] - 2026-11-08

### ✨ Added

- Sync keeps your library, progress, history and settings the same on every device, through a sync server you run.
- The readers and the player offer to go where another device got further.
- Profile locks and protected settings ask for your computer's login password.
- Books can be read aloud through Speech Dispatcher.
- A touchpad pinch zooms and moves the video picture, and Ctrl with the arrow keys pans it.
- Advanced settings offer progressive image loading and a custom display profile for manga pages.
- The setup guide asks how you start, restores a backup inside the guide, and can link the device to sync.
- The library, the Extensions tab and backup restore offer to install the extensions your titles need.
- Intro and credits skip times also come from TheIntroDB.

### 🔄 Changed

- Every migration starts with everything to carry over selected, and only Delete downloads is remembered.
- The player shows the time and chapter in its button row, the skip button above the timeline, and the timeline's preview over the picture.
- Subtitles stay in place when the player's controls show or hide.
- The player's controls hide after a while when paused and as soon as the pointer leaves the window, and stay up only while the pointer rests on one of them.
- Scrolling over the player changes the volume by 1 instead of 5.

### 🧩 Improved

- Migrating a title moves its reading time to the title that replaces it.
- While another episode opens, the player's loading pill can cancel it.
- Opening a book, chapter or episode shows which step it is on.
- When the speech engine can't speak, the readers and the speech preview say why.
- On wide windows, the Comfortable list display shows its rows in columns.

### 🗑️ Removed

- Tracker support, along with the tracking data it kept.
- The native Wayland option. The app always runs through X11, on Wayland through Xwayland.
- Extensions built with Entry SDK 3.x no longer load.

### 🐛 Fixed

- The library's play button and Updates open the player instead of crashing.
- Closing the book reader records its time read.
- Switching the player in and out of full screen leaves its controls shown or hidden as they were.
- The library and other screens keep updating after a database error instead of freezing until a restart.
- Migrating a merged title with seasons to a source that lists every episode together carries each season's progress to its own episodes.
- A dub or server the source doesn't offer for an episode says so instead of playing the previous one again.
- The subtitle editor's preview shows the subtitles where they will appear.
- Reopening a book in a scrolling mode returns to the line at the top of the screen, not half a screen further.
- Covers that fade in are no longer slightly blurry.
- Settings keep the open page and its dialogs when the window switches between one and two panes.

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

[Unreleased]: https://github.com/pa2x2/katari/compare/desktop-v1.0.0-beta.5...HEAD
[1.0.0-beta.5]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.5
[1.0.0-beta.4]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.4
[1.0.0-beta.3]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.3
[1.0.0-beta.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.2
[1.0.0-beta.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.1
[0.1.0-alpha.4]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.4
[0.1.0-alpha.3]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.3
[0.1.0-alpha.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.1
