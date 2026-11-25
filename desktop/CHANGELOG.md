# Desktop Changelog

## [1.3.0] - 2026-11-25

### ✨ Added

- Default subtitle look settings.

### 🔄 Changed

- A manga page that fails to load opens in the app's web view instead of the browser.

### 🧩 Improved

- The mouse drags destinations in the navigation editor without holding first.
- Drag handles in reorderable lists show the move cursor.

### ⚡️ Performance

- Syncing a large library is much faster, most of all on a device's first sync.

### 🐛 Fixed

- A device new to sync shows the full chapter list of each library entry, and its first refresh no longer fills Updates with old chapters.
- The destination dragged in the navigation editor stays under the pointer when settings show in two panes.

## [1.2.0] - 2026-11-24

### ✨ Added

- A Display panel on every cover screen: library, catalogs, feeds, library search, related entries and migration search.
- Ctrl + wheel or a touchpad pinch resizes covers in a grid.
- Cover-only grid and cover size in catalogs, feeds and related entries.
- Extra small cover size.

### 🔄 Changed

- Catalogs and feeds share one layout, and a source can keep its own.
- The Auto cover size is replaced by Medium.
- Cover grids use the full window width.

### 🐛 Fixed

- Covers from single-type sources no longer show a type badge, and type badges follow the Entry type badge setting.

## [1.1.5] - 2026-11-23

### 🧩 Improved

- Changed on other devices says what each change was, such as the old and new value or the filter that was turned on.

### 🐛 Fixed

- Signing in to sync no longer fails when you have reading time in chapters that were later deleted.
- Duplicate detection's settings show up in settings search, and What syncs lists them so you can choose them one by one.

## [1.1.4] - 2026-11-22

### ⚡️ Performance

- In the Long strip and Long strip with gaps modes, upcoming pages are decoded ahead, so tall pages show as soon as they scroll into view instead of staying blank during a fast scroll. Pages leaving the screen no longer make the scroll stutter.

## [1.1.3] - 2026-11-20

### 🐛 Fixed

- Recap pages that show covers appear at full size instead of shrunk.

## [1.1.2] - 2026-11-20

### 🐛 Fixed

- Video no longer skips a stretch or falls out of step with its audio when the host briefly refuses a segment or stops sending one partway.
- WebView no longer repeats a site's check over and over once the pass from an earlier check has expired.
- Recap pages too tall for the system's fonts shrink to fit instead of cutting off their last lines.
- The recap story fills the whole window. On wide windows the side navigation no longer stays beside it.

## [1.1.1] - 2026-11-16

### 🐛 Fixed

- You can pause a video while it is still loading, by clicking the loading indicator or pressing Space.
- Signing in through a site's popup in WebView, such as Google's, works. The popup hands its result back to the site that opened it and closes when it is done.

## [1.1.0] - 2026-11-16

### ✨ Added

- The player plays at any speed from 0.25x to 4x.
- Extra dim makes the picture darker than the screen's own brightness. Set it in the player's settings, with Shift+↑ and Shift+↓, or with Shift and the scroll wheel.

### 🔄 Changed

- The AppImage carries Chromium again, so WebView, site checks and source pages work from the first start. There is no Set up WebView step, download or WebView engine settings page any more.
- Clear WebView data is back in Settings › Advanced › Network.
- Holding the video plays it at twice the speed you chose, instead of always at 2x. Holding Space does the same, and a short press still plays or pauses.
- Manga, anime and books have new type icons: a comic page, a TV and an open book.
- A full resync waits for an unmetered network when changes do, unless Use metered networks for changes is on.

### 🧩 Improved

- Changes made offline sync as soon as the network is back.
- Disconnecting from sync takes effect at once, even offline. Your other devices stop listing this one once the server can be reached.
- Changes made while syncing is paused, or while they wait for an unmetered network, count as unsent when you disconnect, and the warning says how to send them first.
- The sync page says when synced changes wait because a device's clock is off. Setting the time automatically on every device fixes it.
- The sync page lists covers and fonts another device added that never reached the server.

### 🐛 Fixed

- If Chromium doesn't start within 30 seconds, the page waiting for it closes instead of the app crashing.
- Reading on a device whose clock is behind no longer loses to an older position synced from another device. At the first sync, the position read last is kept.
- An entry you add back, or migrate back to, after migrating away from it stays in the library on your other devices too.
- When two devices answer the same migration question differently, the later answer decides on every device.
- Changed on other devices lists a setting for 14 days after it reaches this device, even when the change itself is older.
- Keep mine is no longer offered for a setting you changed here after another device's change arrived.
- A chapter or episode renamed by its source or on another device keeps its download.
- The library's download badge counts only downloads of chapters and episodes the entry still has.
- Badges that don't fit beside a cover's unread count move to a second line instead of covering it.

## [1.0.2] - 2026-11-15

### 🐛 Fixed

- After the computer wakes from sleep or changes networks, the first requests to a source no longer hang until they time out.
- Video no longer plays out of step with its audio track after a short network drop. The player asks again for what failed, and if it still can't get it in time, playback stops with an error and keeps your place.
- Scrolling the feed switcher with a wheel or touchpad switches to the feed it turns to.

## [1.0.1] - 2026-11-15

### 🧩 Improved

- Statistics is easier to read. Each section leads with one large figure, and the Overview groups its cards by what they cover: today, the chosen period, the last 12 months and the library.
- The period's total, its chart and its split by type share one card, which shows a bar's own figures when you select it.
- Activity lists show each title once a day, under a header with the day's total. Select a title's count for its episodes or chapters, or a day for its sessions. A title's own list shows each chapter or episode once a day and marks the ones finished that day.
- A title's Your activity sheet shows the time spent, your progress and the last 12 months, with See activity for each day.
- Recaps lists the years first, then each finished month with the time spent in it.

### 🔄 Changed

- Progress and By media are now one Library card, and Activity patterns is now When you read.
- With a daily goal set, today's time and the goal streak show at the top of the Overview instead of replacing the daily average.

### 🐛 Fixed

- Earlier, from history, adds up to the same total as its list of titles. A title with more recorded sessions than history no longer lowers the total or drops a type from it.
- Updating a few picked entries no longer changes when Updates says the library was last updated, and no longer hides a newer update of the whole library from another device.
- The player's skip button sits on the timeline instead of high above it, and steps aside while the timeline previews a frame.

## [1.0.0] - 2026-11-13

### ✨ Added

- Subtitle timing in the player's settings moves subtitles earlier or later, in 0.1 s steps.
- Ctrl+W closes the window, as its close button does.

### 🔄 Changed

- A title's own reader or player value syncs only while its setting syncs on this device.

### 🧩 Improved

- What syncs lists the settings you chose one by one.

### 🗑️ Removed

- Scrolling sideways over the player, with a tilting wheel or a touchpad, no longer seeks. Drag the timeline or use the arrow keys instead.

## [1.0.0-beta.13] - 2026-11-12

### 🐛 Fixed

- Various sync fixes

## [1.0.0-beta.12] - 2026-11-12

> Sync in this version needs sync server 1.1.0. Update the server first, or sync stops until you do.

### ✨ Added

- Closing the window keeps Katari running in the tray. `Keep running` in the tray in Settings > General turns this off.
- `Start at login` opens Katari hidden in the tray when you sign in.
- Download on another device asks another device on your sync account to download chapters. The other device downloads them once the entry is in its library and shows a notification for chapters it couldn't download. A device that is offline gets the request when it next syncs, within a week. Both devices need this version.
- Settings > Touchpad sets natural scrolling for the whole app: as the system sets it, on, or off.
- The top of Updates shows a running library update's progress and what it has found so far, with Cancel.

### 🔄 Changed

- A new General settings page holds the app language and the date and time formats, which were in Appearance, and the tray switches.

### 🧩 Improved

- A touchpad swipe over the player changes the volume or seeks as far as your fingers move, follows them when the system scrolls naturally, and stops when they lift.
- A touchpad swipe pans a zoomed image as far as your fingers move, and an up or down swipe over a sideways row scrolls the page.
- Right-clicking a chapter's download button opens its menu instead of the chapter's.
- The update report shows a running update's progress, the entries it is checking and Cancel. A finished report says how long the update took and how many entries it checked.
- When one error fails entries on several sources, the update report lists it once with one retry, and Failed offers Retry all.
- Update report rows show the entry's type and source, and new chapters are named and open as they do in Updates.
- "An update is already running" offers View, which opens the running update's report.
- The Sync page lists your devices as soon as it opens and marks the ones connected now.

### 🐛 Fixed

- A wheel or touchpad scroll no longer leaves a large title or a sheet half open, and scrolling up at the top of a list opens the large title.

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

[Unreleased]: https://github.com/pa2x2/katari/compare/desktop-v1.3.0...HEAD
[1.3.0]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.3.0
[1.2.0]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.2.0
[1.1.5]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.1.5
[1.1.4]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.1.4
[1.1.3]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.1.3
[1.1.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.1.2
[1.1.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.1.1
[1.1.0]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.1.0
[1.0.2]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.2
[1.0.1]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.1
[1.0.0]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0
[1.0.0-beta.13]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.13
[1.0.0-beta.12]: https://github.com/pa2x2/katari/releases/tag/desktop-v1.0.0-beta.12
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
