# Desktop Changelog

## [1.0.0-beta.11] - 2026-11-11

### ✨ Added

- Pressing and holding the sync icon syncs at once.

### 🧩 Improved

- AniSkip and TheIntroDB find intro and credits times for later seasons, reading the season from the source's season groups or episode names.
- You change a service's intro times match on one page that picks the series, season and episode and shows its times before you use them.
- The player's episode list, the manga reader's chapter list and the book reader's table of contents open with the current item in the middle.

### 🐛 Fixed

- In the long strip, a page loading above the one you're reading no longer pushes it down, and a page you scroll back to keeps its height.

## [1.0.0-beta.10] - 2026-11-11

### 🔄 Changed

- Settings, hints and notifications say click, press and hold, and row buttons where they said tap, long press and swipe.
- Sync, download, reader and translation settings and statuses talk about metered and unmetered networks instead of Wi-Fi and mobile data.
- Sync shows this device as a computer, and icons that pictured a phone or a tapping hand show a computer or a mouse.
- Settings no longer mention a secure screen or recap sharing the desktop app doesn't have, and Share crash logs is now Save crash logs.

### 🗑️ Removed

- The manga reader's Show actions on long tap setting. Holding the mouse button drags the page, and a right click opens its actions.

## [1.0.0-beta.9] - 2026-11-10

### ✨ Added

- Settings has a Notifications page with a switch that turns every notification off, one for each kind of notification, and Hide notification content, which moved there from Security and privacy.
- Profile locks and protected settings also take a fingerprint on systems whose own sign-in doesn't read the fingerprint reader.
- A touchpad pinch zooms and moves the manga reader's page or strip.
- The Library, the Extensions tab and the extensions sheet offer to install or uninstall the extensions you installed or uninstalled on your other devices. Installed extensions in What syncs turns this off.
- Katari loads extensions built with SDK 4.1, whose sources can use sites that only work with their scripts running.

### 🧩 Improved

- Touchpad scrolling follows your fingers and glides on after a flick, instead of moving in mouse-wheel steps.
- Scrolling or clicking to turn pages in the readers keeps the pointer hidden, and nudging the mouse no longer brings it back in the readers, the player or the immersive feed.
- The picture-in-picture window is titled "Katari Picture-in-Picture" and the episode, so window rules can tell it from the main window.
- A chapter read or marked as read on another device has its download deleted by your after-reading settings, as one read on this device does.
- The automatic background reaches the long strip's sides, taking the colours of the gaps between panels down each page, and fills the space around a two-page spread.
- A wide side sheet's content fills the sheet instead of a narrow column in its middle.

### 🐛 Fixed

- Your desktop's own notification settings for Katari now apply to its notifications.
- A wheel or touchpad swipe on the feed switcher opens the feed it turns to, instead of moving only the switcher.
- The manga and book readers no longer count reading time while their window is out of focus.
- Long strip pages meet without a line between them at any zoom.
- With the page slider at the side, the reader's toolbar fits its buttons instead of stretching across the page with the buttons at one end.
- A source's immersive view hides the side dock, and switching tabs while titles are selected no longer leaves the navigation hidden.

## [1.0.0-beta.8] - 2026-11-10

### ✨ Added

- Settings › Advanced has a WebView engine page that shows, downloads and removes Chromium.

### 🔄 Changed

- The AppImage no longer carries Chromium. The first source that needs WebView asks to download Chromium once, and a source that needed it before then says so in the Library and in a notification.
- A double-click switches full screen without pausing the video, in the player and in feed clips.

### 🧩 Improved

- In the subtitle editor, you drag subtitles by their lines or move them with the arrow keys, they snap to guides, and a position grid has a Recenter button.
- Pressing E, ? or S again closes the episode list, the shortcut list or the manga reader's area selection.
- The app's icon shows from launch until the window opens.
- WebView pages send the platform and browser hints that match their user agent, so sites ask less often to prove you're human.

### 🐛 Fixed

- Previous and Next over a video now dim while another episode opens.
- A stream with separate audio shows loading until its audio is in, instead of a still picture, and its subtitles no longer delay the start.
- WebView starts again after Chromium fails to start or stops, instead of staying unavailable until the app restarts.

## [1.0.0-beta.7] - 2026-11-09

### ✨ Added

- The immersive feed goes full screen with F or a bar button.
- Clips in the immersive feed play at the player's volume, which Shift+↑/↓ and a slider on the sound button change.
- The desktop's media controls and keys play and pause the shown clip in the immersive feed.

### 🔄 Changed

- The immersive feed no longer saves progress or adds to History.
- Space and K play or pause in the player without bringing up hidden controls.

### 🧩 Improved

- The immersive feed fills the window, its controls show and hide with the mouse as the player's do, a click plays or pauses a clip, and buttons step to the previous or next title.
- Tall media in the immersive feed has the title and its actions in a panel beside it.
- Long-strip chapters in the immersive feed read down a column that the wheel scrolls.
- The immersive feed shows a loading title's cover and name, loads the next two titles ahead, and offers Next title when media fails to load.
- Enter, D and S work the shown title's actions in the immersive feed, and clip timelines always show the time played.
- Sheets open at the side of the window you pressed and each has a close button, and short choices such as the profile switcher open beside their button.
- In large windows, side sheets lay out their rows in two columns.
- Extension filter pills open the filter, and the filter can search languages.
- The player's skip button stays readable over dark credits.

### 🐛 Fixed

- Tab in an open sheet, dialog or reader panel now reaches its controls.
- A video paused for several minutes no longer drops frames shortly after it resumes.
- Settings pages opened from another page in two-pane Settings now have a back button.
- Statistics lists no longer run under the side dock.
- The last card on a Settings page no longer touches the bottom of the window.

## [1.0.0-beta.6] - 2026-11-09

### ✨ Added

- History names the other device a title was last read on.

### 🔄 Changed

- Background sync runs every 30 minutes unless you pick another interval.

### 🧩 Improved

- Changes reach your other devices closely to real-time instead of after a pause of several seconds.
- Changes from other devices arrive within seconds while the app is open, even when its window is minimized or another window has focus.
- The sync status icon also shows on Updates, History and Statistics, and at the foot of the side dock on wide windows.

### ⚡️ Performance

- Sync no longer checks for changes every few seconds while the app is open with nothing to send, and each run reads less of the database.

### 🐛 Fixed

- An entry's cover in the merge sheet looks the same on every visit.
- Grid scrollbars, such as the library's with a Pinned shelf, no longer run past the end of their track.

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

[Unreleased]: https://github.com/pa2x2/katari/compare/desktop-v1.0.0-beta.11...HEAD
[1.0.0-beta.11]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.11
[1.0.0-beta.10]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.10
[1.0.0-beta.9]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.9
[1.0.0-beta.8]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.8
[1.0.0-beta.7]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.7
[1.0.0-beta.6]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.6
[1.0.0-beta.5]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.5
[1.0.0-beta.4]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.4
[1.0.0-beta.3]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.3
[1.0.0-beta.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.2
[1.0.0-beta.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.1
[0.1.0-alpha.4]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.4
[0.1.0-alpha.3]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.3
[0.1.0-alpha.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v0.1.0-alpha.1
