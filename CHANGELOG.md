# Changelog

## [2.0.0] - 2026-10-26

Katari now installs Aniyomi extensions and LNReader plugins, and can play video with the optional mpv add-on.

### ✨ Added

- Aniyomi anime extensions can be installed from Aniyomi stores to browse, watch and download episodes.
- LNReader plugins can be installed from an LNReader plugin index to browse and read novels.
- Extensions › ⋮ › Extension compatibility lists which kinds of extension this Katari loads.
- Adding a store that doesn't sign its extensions asks first, and what it installs is marked Not signed.
- Allowed content warnings has a Not rated choice for extensions their store doesn't rate.
- Extension info has a Clear data action for the extension's settings and logins in the active profile.
- Source rows name the store an extension came from beside its language.
- Tapping the source of an entry whose extension isn't installed offers to install it or to migrate the entry.
- A Player row in the video player's settings picks Auto, Built-in or mpv for each title.
- Episodes can offer several servers, and the player moves on to the next one when a server fails.
- Episodes can have separate audio tracks, picked in the Audio row and kept in downloads.
- Intros, recaps, credits and previews can be skipped from the timeline, with times from the source or AniSkip.
- Skip intro and credits has a Show a button, then skip mode.
- Swiping sideways on the video seeks.
- The volume swipe can boost the volume up to 150%.
- The video player can Fit, Fill or pinch-zoom the video, remembered for each title.
- The episode's chapters show under the timeline and can be jumped to from the player settings.
- Picture-in-picture has previous and next episode buttons.
- Subtitle text can have an outline, a shadow or no edge.
- The video player's settings menu has a Default player settings shortcut.
- DASH episodes can be downloaded and watched offline.
- Titles that an extension splits into seasons open as one merged entry, with the seasons locked in Manage merge.
- The episode filter can hide filler episodes.
- The book reader can search the open chapter and all downloaded chapters.
- Text rules hide or replace text that repeats across a series' or a source's chapters.
- You can set the book reader's line spacing, margins and text alignment, including justified text.
- You can pick the book reader's font, including TTF and OTF files you import.
- You can make your own book reader colours from Theme › New colours….
- Show time and battery shows them under the book reader's page while the status bar is hidden.
- The book reader's top bar has a Bookmark button.

### 🔄 Changed

- When two extensions provide the same source, the first keeps it and the other is marked Conflict, with Use this one to switch.
- Video player problems offer the action their message suggests, such as Try another server, Change dub or Extensions.
- A slow episode load says what it is waiting for and lets you switch server, dub or quality.
- A switch that fails during playback shows a card with Retry and Change instead of a toast.
- The video player opens at the brightness you last set, unless Remember brightness is off.
- For merged entries, the video player's title names the series, with the season and episode under it.
- Interrupted HLS and DASH downloads resume where they stopped.
- Downloads pick the same stream the player would for the title.
- Books show their own colours, boxes, borders and layouts, which Theme › Publisher styles turns off.
- Books use the fonts their chapters name, including web fonts.
- Very long web novel chapters now open.
- Prepare next chapter is on by default in the book reader.
- When refreshing an entry fails, the error offers WebView.

### 🗑️ Removed

- Extensions built with Entry SDK 2.x or Tachiyomi extensions-lib 1.4 no longer load.
- The Split tall images download option.

### 🐛 Fixed

- Installed extensions that no store lists stay visible while a store filter is on.
- An extension that fails to update keeps its installed version.
- The Shizuku installer recognises Sui.
- The built-in player plays DASH videos instead of crashing.
- The video player no longer crashes on videos wider than 2.39:1.
- Videos with an unusual playlist address now play and download.
- A dub or quality that fails to play is no longer remembered for the title.
- Closing picture-in-picture pauses the video unless Keep playing in the background is on.
- An episode that fails to load no longer loses its saved length.
- With Auto-play next on, a next episode that fails to open is no longer reopened over and over.
- HLS downloads keep only the chosen quality instead of every quality.
- Episode downloads include the subtitle the player would show.
- Changing the dub in the download sheet lists that dub's qualities and subtitles.
- Downloaded episodes stay playable after their extension is uninstalled.
- Episode downloads from same-named sources in different languages no longer share a folder.
- An episode download that fails unexpectedly says why.
- A chapter that finishes downloading while Updates refreshes no longer shows as queued.
- A download dragged in the Download queue stays inside its source's card.
- Covers from book and video sources that need the site's headers now load.
- Book images now load when the site sends no file type or redirects to another server.
- AVIF images in books show on every Android version.
- A book chapter download no longer fails because one of its images is gone.
- Book chapters with an empty image open and show the image's description.
- Scrolling back up to a picture in a book no longer makes the page jump.
- Restoring a backup no longer removes titles you added to a merge after the backup was made.
- Typing in the merge target search no longer flashes "nothing matches".
- Resume, the video player's episode list and Skip filtered chapters follow the entry's chapter filters.
- Random library order stays put when an entry is hidden or added.
- Covers no longer reload when you remove an entry from the library.
- A successful chapter refresh clears the entry's failed checks in the Update report.
- A source's catalogue ends when a page brings nothing new instead of loading without end.
- Automatic backups, library updates and recaps run on time again after the device clock was set back.
- Future times such as Next automatic update no longer show as "just now".
- Automatic backups of different Katari builds sharing a folder no longer delete each other.
- WebView no longer crashes on sources that send an Origin header.
- WebView sends a source's headers only to that source's site.
- The screen stays on while a video plays in an immersive feed.
- Duplicate detection's Title exclusions no longer crashes when the list is empty.
- On narrow screens, rows move their controls under the title instead of breaking it mid-word.
- Rows that cut a long description end it with "…".

### ⚡️ Performance

- Backups of large libraries are created and restored much faster and no longer run out of memory.
- Library updates check the sources with the most entries first.
- HLS and DASH downloads fetch four segments at a time.
- Downloaded episodes open faster.
- Extensions load faster at startup when many are installed privately.

## Based on [Mihon 0.21.3](https://github.com/mihonapp/mihon/releases/tag/v0.21.3)

## [1.17.1] - 2026-10-22

### 🔄 Changed

- Messages about the current screen show as snackbars instead of toasts.
- Searching for a merge target puts the search field on top, lists results with covers, and says when nothing matches.
- Long-pressing a catalogue entry opens its actions as a menu at its cover.

### 🐛 Fixed

- Manga chapters that finished downloading just before the app closed no longer show as not downloaded.
- Book downloads no longer show "Checking downloads" while they load from their index.
- Dot badges on the dock show as small dots instead of empty pills.
- On a first merge, the merge editor lists the entry you are adding first, as it does for existing merges.

## [1.17.0] - 2026-10-21

### ✨ Added

- The video player can keep playing in the background with a media notification when Keep playing in the background is on.
- You can pick a video stream's audio track in the video player's settings. Only if stream provides multiple tracks.
- Tapping the video player's time switches it to the time remaining, and the choice is remembered.
- Backup and restore let you pick which profiles to include, and ask once to unlock locked ones.
- With more than one profile, the active profile's avatar sits in the top bar of each main screen and opens the profile switcher.

### 🔄 Changed

- Global UI overhaul of the application.
- A floating dock replaces the bottom bar.
- Updates and History show one card per day with portrait covers.
- An expanded update group lists its chapters under the entry's title instead of repeating the entry.
- The feed switcher floats above the dock as a pill.
- Every place that switches profiles opens the same sheet, and profiles are managed in Settings › Profiles.
- Speed, quality and subtitle picks in the video player apply on tap, and only a source change needs Apply source.
- In landscape, video player settings open as a side panel.
- Episode skipping, speed and subtitles moved under the video player's timeline.
- Brightness and volume swipes start anywhere on their half of the video player.
- The picture-in-picture button is always shown, and its setting only controls entering it when you leave the app.
- Video player errors explain what went wrong and offer Retry, WebView and Change source, with the details folded away.
- The feed's back-to-top button appears only after you scroll back up a good distance.
- The profile picker shows which profiles are locked, and the unlock prompt names the profile.
- Restoring a backup no longer switches the active profile, and its notification names the restored profiles.

### 🐛 Fixed

- When the next episode fails to open, the video player keeps the current one and shows a retry card.
- The video player's title, time and controls stay readable over bright scenes.
- The video player's episode list shows the time left on episodes in progress and no longer repeats the series title.
- The video player's title names the merged member that owns the episode.
- The unlock button stays in the lock button's place when the video player's WebView menu is open.
- Opening WebView from the video player no longer sends the video into picture-in-picture.
- Episodes from video sources that need their scanlator or source-specific data now play and download.

## [1.16.4] - 2026-10-20

### ✨ Added

- Chronological feeds show a row where a refresh stopped catching up, with a button to load the items it missed.
- Chronological feeds show a "Seen before" line above the newest item you had already seen.

### 🐛 Fixed

- Refreshing a chronological feed that fell far behind stops after 10 pages instead of paging without end, and shows new items as each page arrives.

## [1.16.3] - 2026-10-20

### ✨ Added

- Starting a library update while another profile updates queues it instead of refusing it.
- Each profile gets its own library update notification channel and summary.

### 🐛 Fixed

- Library updates and downloads stay on the profile that started them after you switch, and use that profile's extension logins.
- Tapping a notification that switches profile while Katari is open now opens its target.
- Global and migration search stay at the top while sources answer and results reorder.

## [1.16.2] - 2026-10-16

### 🐛 Fixed

- The book reader marks a chapter read once you reach its end, not only when the next chapter opens.
- A book chapter opened from the contents shows a loading screen, and a Retry dialog if it fails to load.
- After Android closes Katari in the background, the book reader reopens the chapter you were on instead of the one you started from.
- Links and contents entries in the scrolled book reader put their target at the top of the screen.
- The book contents list keeps untitled pages, such as the cover, in reading order and drops the quotes around their titles.
- Jumping to the spot already on screen no longer adds a return point in the book reader.
- Copy, Listen and Translate come first in the book reader's text selection toolbar instead of landing in its overflow menu.
- Tapping a book image follows the paged reader's tap zones instead of always showing or hiding the menu.

## [1.16.1] - 2026-10-15

### 🐛 Fixed

- Source headers in global search show their arrow again.
- In the manga reader's translate mode, the translation popup no longer covers the floating toolbar.

## [1.16.0] - 2026-10-15

This is a technical release. Metro replaces Injekt for dependency injection, the Feature graph is gone, and every screen now uses one UI kit.

### 🔄 Changed

- Settings rows, section headers and sheet content use the same side margin on every screen.
- Confirming a delete or discard shows a red button next to Cancel.
- Bottom sheets lost their drag handle, and details sheets lost their Close button.
- Navigation rows no longer end in an arrow.
- Text-to-speech, translation and text recognition settings are ordinary settings lists, each with the same engine card.
- Success, warning and chart colours are tinted toward the theme's primary colour.

### 🧩 Other

- A missing or conflicting dependency binding now fails the build instead of crashing the app.
- Extensions still look up their services through Injekt, which now reads them from the Metro graph and is read-only.
- Manga, anime and book register their features in keyed Metro maps instead of the Feature graph.
- Removing, migrating, merging and backing up entries call their follow-up steps directly, and so do media-session changes.
- `presentation-core` is now a strict UI kit. A build check rejects Material, raw colours, text styles and dp values in every other module.
- Reader chrome moved from `presentation-core` to its own module, `entry-viewer-chrome-ui`.

### ⚡️ Performance

- R8 can now merge app classes, and the baseline and startup profiles match the new code.

## [1.15.1] - 2026-10-14

### ✨ Added

- Translation, text recognition, profiles, merging and the Statistics layout editor have help buttons that open their guide page.

### 🔄 Changed

- Help links open Katari's guide on its website instead of Mihon's documentation.
- Loading indicators use the Material 3 Expressive style.

### 🗑️ Removed

- Mihon's donation screen and the donation prompt it showed every six months.

### 🐛 Fixed

- Manga downloads use Mihon's folder names, so Katari finds the downloads in a Mihon storage folder.
- Trackers other than Komga, Kavita and Suwayomi no longer mark chapters read from their progress.
- The arrows on the Statistics activity chart move by the whole selected period instead of one bar.
- The Settings action on the migration search screen opens the source's settings instead of the help site.
- Occasional manga reader crash that happened when you leave it or turn the screen off.

## [1.15.0] - 2026-10-13

### ✨ Added

- Merged library entries show their member count on the cover, which can be turned off.
- History and Updates rows name the merged member a chapter comes from.
- Members of a merged entry can be collapsed on the entry screen and in the player, and show their chapter counts and sources.

### 🔄 Changed

- Icons throughout the app use the rounded Material Symbols style.
- A merged entry's header shows its member count and opens Manage merge instead of listing every title.
- The Updates tab no longer shows a snackbar when an update finishes, since its header already sums it up.

### 🐛 Fixed

- Merged entries from several sources name those sources instead of showing "Multi".
- Chapters of a merged entry use their own member's page language and translation settings.
- Each profile keeps its own AniList score format, so logging in on one profile no longer changes another's.
- The entry screen offers to resume an entry read partway through its first chapter instead of starting it.
- Global search keeps every source's results when several finish at once, and drops results from a replaced search.
- Book chapters from some sources no longer fail to open.
- A newly added feed no longer crashes while loading.
- Snackbars keep their margin from the screen edges and show their action in the usual color.

## Based on [Mihon 0.21.2](https://github.com/mihonapp/mihon/releases/tag/v0.21.2)

## [1.14.2] - 2026-10-11

### 🧩 Improved

- The completion tile in Statistics and activity history names what it counts, such as chapters or episodes, instead of "Completed".
- In Statistics, that tile also shows the average time per chapter or episode, or the date you last finished one.

### 🐛 Fixed

- Refreshing a feed with many new items keeps your place instead of jumping to the newest ones.

## [1.14.1] - 2026-10-07

### 🧩 Improved

- The backup notification counts the entries backed up so far and shows when the file is being saved.

### 🐛 Fixed

- Library update results and snackbars on the entry and cover screens have a close button.

### ⚡️ Performance

- Backups are faster to create and smaller.

## [1.14.0] - 2026-10-06

### ✨ Added

- Statistics has yearly and monthly recaps that play as stories, listed under Recaps in its menu.
- A yearly recap notification arrives on December 1 with the year so far and on January 1 with the whole year.
- An optional notification after each month opens that month's recap.
- Recaps can hide chosen titles, and leave out 18+ titles unless you include them.
- Statistics can share a recap image of the shown period.
- Statistics can track a daily goal with today's progress and a goal streak.
- The entry screen shows your time spent on it, how long the unread chapters should take, and the time spent on each chapter.
- History day headers show that day's time spent and open its activity.

### 🔄 Changed

- Customize moved from the Statistics toolbar into its menu.

### 🐛 Fixed

- The app unlock prompt no longer closes right away and leaves a blank screen.
- App bar menus open below their button instead of covering it.

## [1.13.3] - 2026-10-03

### ✨ Added

- Sources can be paused for library updates for a day, 3 days, a week, or until a chosen date and time.
- The update report lists each paused source with its entries, and can resume it or check it once.
- An entry's Updates sheet shows which of its sources are paused and can resume them.

### 🧩 Improved

- The Updates tab names the sources paused for library updates.
- Settings > Library updates keeps paused sources listed and shows when each pause ends.
- Entries set to never check can be resumed from the update report.

## [1.13.2] - 2026-10-03

### ✨ Added

- The manga reader's translation popup can read the original and translated text aloud.

### 🔄 Changed

- Tapping the engine name in a translation popup changes the engine, replacing the menu button.

### 🧩 Improved

- Translation popups always name the engine that translated the text.
- Translation popups always offer fullscreen, not only when the text is cut off.

### 🐛 Fixed

- Leaving fullscreen returns to the translation popup instead of closing it.
- Changing the language or engine in fullscreen no longer drops back to the popup.

## [1.13.1] - 2026-10-02

### 🔄 Changed

- Updating selected entries or a single library page adds its results to the last full update's report and marks them as rechecked, instead of replacing the report.

### 🧩 Improved

- Failing sources in the update report expand to list their entries and can retry or migrate them together.

### 🐛 Fixed

- Pause buttons in the update report show what is paused and can be tapped again to resume.
- The update report no longer offers Migrate or WebView for entries where they can't work.
- Nothing new in the update report has a section header like the other groups.
- Uninstalled sources show their name from the extension repository instead of an id.

## [1.13.0] - 2026-10-02

### ✨ Added

- Settings > Library updates chooses which categories, sources, and entry types library updates check.
- Categories can have their own update schedule and skip rules.
- An entry's Updates button sets it to always or never be checked and shows what the last update decided for it.
- The latest library update has a report with retry, pause, and WebView actions for failed entries.
- Selected library entries can be checked for updates or given an update mode together.
- The Updates feed can be filtered by entry type and source.
- Upcoming marks entries that library updates won't check.
- App updates download and install inside Katari, with notes for every version since the installed one and an option to skip a version.
- About has a pre-release update channel and a switch for checking on launch.

### 🔄 Changed

- Library update settings moved from Library settings to their own screen.
- An entry in a category switched off for updates is no longer checked, even if it is also in a checked category.
- The library update failure notification opens the update report instead of a text log.

### 🧩 Improved

- A library update names what it covers and sums up the result when it finishes.
- The Updates feed groups three or more updates of an entry from one day into one expandable row.
- The Updates feed says how many updates its filters hide.
- Tapping the large-update warning opens the library update settings.

### 🐛 Fixed

- FOSS releases now include the in-app updater.
- Chapter actions and filters shared by mixed entry types say seen and unseen instead of consumed.

### ⚡️ Performance

- Loading a page in Browse, feeds, or search no longer rebuilds the library.
- Covers that fail to load share one error image instead of drawing it for each cover.

## [1.12.0] - 2026-09-30

### ✨ Added

- Downloaded manga chapters can be translated in the background from the chapter menu, the selection bar, or Updates.
- Chapters with a stored translation open with it already drawn over their pages.
- A series can translate every chapter it downloads, set with "Translate downloads" in the reader's series languages.
- Settings > Downloads > Auto-translate translates new downloads and upcoming chapters, optionally only while charging or on Wi-Fi.
- The download queue has a Translations tab to follow, retry, pause, or cancel chapter translations.
- DeepL is available as a translation engine, through the DeepL API or a compatible server of your own, and translates manga pages in context.
- The library shows a running update's progress and can cancel it.
- Library search shows matches from every group in one list and suggests search syntax.
- An empty library links to Browse sources and backup restore.
- Feeds can be renamed, and removing a feed can be undone.

### 🔄 Changed

- The manga reader's Fullscreen setting is replaced by separate Show status bar and Show navigation bar settings.
- The feed switcher is dragged sideways to change feeds, replacing the arrow buttons.
- Lower library grouping levels appear as chip rows that scroll with the grid.
- Reselecting the Library tab scrolls to the top before opening the settings sheet.

### 🧩 Improved

- Library unread and download badges are easier to tell apart.
- Library covers show their selection state while selecting.
- Library display modes are picked from preview tiles.
- Feeds show their source's language, and the add-feed sheet can search sources.
- Tabbed settings sheets no longer change height when switching tabs.

### 🐛 Fixed

- The filter sheet shows Popular or Latest as the selected preset when the filters match it.
- The Sources app shortcut opens the Sources page of Browse.
- The library's unread filter no longer reads "Unconsumed".
- The library's items-per-row slider follows the device orientation.

### ⚡️ Performance

- LibreTranslate requests no longer decrypt the saved API key every time.

## [1.11.0] - 2026-09-29

### ✨ Added

- The manga reader can recognize and translate text on pages. Turn on translate mode from the bottom bar, then tap a speech bubble or drag over an area to translate it. With "Show translations on the page", the reader translates every recognized text and draws the translation over the page. It can also prepare upcoming pages, optionally only on unmetered networks or while charging. The translate toolbar floats over the page and tucks into a screen edge.
- Text recognition runs on the device. Manga OCR reads Japanese, including vertical text and furigana. PaddleOCR reads English, Latin, Cyrillic, Korean, Chinese, Arabic, Devanagari, Thai, Greek, Tamil, and Telugu scripts. ML Kit is available in builds with Google Play services. Models download only after you approve them, and mobile data use is opt-in.
- Settings now have a Text recognition screen. It lets you choose the engine, see how each language is read and override it, try the settings on an image before saving, and review downloaded models with what each one is used for. Models that no engine needs anymore can be freed.
- Translation languages can now be set per series. Languages chosen in reader translation popups apply to that series, and "Use for all series" makes them the new default. Manga series also remember their page language, which starts from the source's language. Book reader settings show and change the book's translation languages. Settings > Translation > Series languages lists every series with its own languages and clears them one by one or all at once. Backups include these choices.
- Statistics has a twelve-month reading calendar and an activity patterns card with session counts, the usual time of day, and the busiest weekday. Summary tiles compare the period with the previous one and show the daily average and best streak.
- Statistics top titles show covers, completed chapters, and share of reading time, with a See all screen for the full ranking. The All range adds a Lifetime card, and the Earlier screen explains history that has no dates. Library insights show progress as stacked bars and break down genres, status, sources, downloads, and titles added per year.
- The source browse screen shows how many filters are applied, with a chip for each applied filter that removes it. Tapping a chip opens the filter sheet at that filter, and a Changed view lists only filters that differ from the source defaults.
- FOSS releases now include separate APKs for arm64-v8a and armeabi-v7a devices, and the FOSS in-app updater downloads the one that matches the device.

### 🔄 Changed

- Katari now targets Android 17. On Android 17, a connection to your local network, such as a self-hosted LibreTranslate server, asks for the Nearby devices permission. Without that permission the connection fails at once with an explanation instead of timing out.

### 🧩 Improved

- The filter sheet has a status header with the active preset and a count of changed filters. Sort filters show the current option with a separate ascending or descending control. Include/exclude groups appear as chip grids with a pinned search. Short single-choice filters appear as chip rows, and text fields have clear buttons and move to the next field from the keyboard.
- Filter group headers summarize the group's selection, and a reset action appears only when the group differs from its defaults. Invalid fields are marked where they are, a validation bar with Show stays above every filter page, and Apply states why it is blocked.
- Paged filter lists have a header like the rest of the sheet and a Done button that returns to it.
- Translation language pickers show recent languages, the app language, and the profile default first. Languages the current engine cannot translate into from the source are marked with the reason. The Translator tab reopens with the languages it last used.
- Translation and text-to-speech settings collect unsaved changes in a save bar and ask before you leave without saving.
- Manga pages that the reader scales down look smoother, and interlaced PNG pages need less memory to load.

### 🗑️ Removed

- Release APKs no longer support x86 and x86_64 devices. Published APKs, including the universal ones, are for ARM devices only.

### 🐛 Fixed

- Switching from a filtered search to Popular or Latest no longer discards the applied filters, and the filter chip returns to the search in one tap. Toolbar searches now use the applied filters instead of unapplied edits in the sheet.
- A source filter that throws an error no longer crashes the filter sheet. Only that filter reports that its summary and checks are unavailable.
- A failed filter suggestion lookup now says that suggestions could not be loaded instead of reporting an internal error.
- Screen readers now announce the role and state of include/exclude filter rows.
- The book reader now uses the book's declared languages when detecting the language of selected text.
- Snackbar actions stay readable in book reader themes whose accent color matches the text color.
- Cancelling a LibreTranslate translation now stops it even while the response is still downloading.

### ⚡️ Performance

- Translations shown in reader popups and on manga pages are cached on the device, so text translated before appears without asking the engine again, even after the app restarts.

## [1.10.2] - 2026-09-27

### ✨ Added

- Manga reader now has a table of contents in the bottom bar.

### 🐛 Fixed

- Sheets with text fields stay in place when the keyboard opens while they are still sliding in. They also keep the correct padding above system bars and the keyboard.

## [1.10.1] - 2026-09-25

### ✨ Added

- Manga reader options changed inside the reader now apply to that series only. Profile defaults stay in Settings. The dialog shows shared options in a Common tab, and Reset clears only the current series' overrides.

### 🐛 Fixed

- Entering a locked profile now follows its Lock when idle setting. Switching back within the delay no longer asks again, while longer gaps and restarts still require authentication.

## [1.10.0] - 2026-09-19

### ✨ Added

- Manga and book readers can now show the chapter transition card `Always`, only `When needed`, or `Hidden`. Hidden mode replaces the card between contiguous chapters with a compact loading indicator while the next chapter loads, keeping the full card for chapter gaps, load failures, and the end of content. Existing "always show chapter transition" choices carry over.
- The Translator's language pickers now show recently used languages as one-tap chips above the list, remembering the last six per profile.

### 🧩 Improved

- Translation language lists now show each language's native name beside its code, and searching matches native names and ignores accents, so `espanol` finds `Español`.
- The Translator's swap action now works from explicitly chosen languages even before a translation completes, and explains when the current pair cannot be swapped.

## [1.9.0] - 2026-09-17

### 🔄 Changed

- Library updates for manga, anime, and books now arrive in one grouped notification under a single Library updates channel.

## [1.8.6] - 2026-09-12

### 🐛 Fixed

- Paged source filters now stay in sync with edits and reloads instead of showing stale selections.

## [1.8.5] - 2026-09-10

### 🐛 Fixed

- Book reader text-selection menu now follows translation popups as they appear, grow, or close, and stays with the selected text when the popup is hidden.
- Book reader table of contents no longer shows a duplicate nested entry for the current chapter.

## [1.8.4] - 2026-09-10

### 🐛 Fixed

- Book reader text-selection menu no longer blinks when translation results arrive without changing its position.

## [1.8.3] - 2026-09-09

### 🐛 Fixed

- Book reader text-selection menu now moves when a growing translation popup would otherwise cover it near the top or bottom edge.

## [1.8.2] - 2026-09-07

### 🐛 Fixed

- Restored the book reader's text-selection menu when enabled, including the Copy action.
- Book reader page taps now keep working after an adjacent chapter finishes loading.
- Book table headers now preserve authored text direction, line spacing, and indentation without overlapping the next row.
- EPUB books now respect text and stylesheet encodings and correctly resolve resource names and links containing emoji or other supplementary Unicode characters.
- Failed manga chapter navigation now preserves the current page and the existing `Return` position.
- Editing filters or saving a preset after a new search now keeps that search instead of restoring the previously applied preset's query or listing mode.
- Download queue rows now reflect sorting and move-to-top or move-to-bottom actions, including while downloads are paused.
- Statistics month navigation now preserves the intended day across shorter months and restores the correct date when navigating back.
- Earlier activity in Statistics now identifies activity without detailed records instead of showing a misleading cutoff date.

## [1.8.1] - 2026-09-06

### 🐛 Fixed

- Book reader volume keys now keep turning pages after text selection.
- Book reader page counters no longer reset when switching between left-to-right, right-to-left, and vertical paged reading.

## [1.8.0] - 2026-09-06

### ✨ Added

- Read and download reflowable EPUB books from supported sources.
- Book reader now offers left-to-right, right-to-left, and vertical paged reading, with configurable tap zones, page transitions, and volume-key navigation.
- Book reader now has a position slider and direct page or percentage entry for jumping within a chapter or EPUB section.
- Book and manga readers now offer a `Return` action to restore the position before the most recent navigation jump.
- Statistics cards can now be reordered or hidden, with separate layouts for each profile and tab.
- Extensions can now be filtered by store.
- Supported source filters now offer date pickers with year, month, or day precision and validation before searching.

### 🧩 Improved

- Source filters now show active selections, collapsible group summaries, and group reset controls. Saved presets preserve selections across filter reordering and renaming when the source supports it, and prompt for repair when a saved selection no longer matches.

### 🔄 Changed

- The Statistics year chart now shows monthly totals, with partial months limited to the selected date range.

### 🐛 Fixed

- Queuing manga, anime, or books while another media type is downloading now starts the new downloads without interrupting the active transfer. Download queue rows also keep their progress up to date.
- Book translation now waits until you release the text selection and starts without an extra delay.
- Statistics durations shorter than a minute now display as less than a minute instead of zero minutes.

## [1.7.0] - 2026-09-01

### ✨ Added

- Added a home-navigation editor that lets you reorder destinations, move them between the primary bar, More menu, and hidden list, and choose a startup destination.
- Added Translator as an optional home destination for translating typed text with configurable languages and engines, detected source languages, result swapping, copy and share actions, and text-to-speech playback.
- Added Statistics as an optional home-navigation shortcut.

### 🧩 Improved

- Automatic language selection for book-reader translations now uses the selected text's context, the book's declared languages, and recently recognized languages for more reliable results.

### 🔄 Changed

- Katari now requires Android 10 or later.

### 🐛 Fixed

- Statistics activity-chart scales now update to match the visible period while scrolling.
- Translation popups now remain anchored to the selected text while their content is remeasured and no longer slide horizontally.

## [1.6.0] - 2026-08-28

### 🧩 Improved

- Reworked the Statistics dashboard with clearer activity and current-library sections, including media-specific progress and insight views, top titles, and session patterns.
- Activity charts now show clearly labeled daily, weekly, monthly, or yearly totals without a trend line.
- Book reader keeps the reading position stable while adjacent chapters load, and lets manual scrolling override pending navigation.
- Adaptive feed layouts and back-to-top controls now track the current window and scroll state, while Statistics, activity history, and the Upcoming calendar refresh their date formatting when the locale changes.
- Legacy extensions now load correctly on older Android versions.

### 🐛 Fixed

- Statistics and activity history no longer let accidental sessions shorter than 10 seconds inflate timed totals; consumption completions from short sessions remain visible in history and completion counts.
- In-app release notes now render GitHub line endings correctly and omit checksum and download-selection text intended for release downloads.
- WebView renderer termination now closes the affected page safely instead of leaving the app in a crashed state.
- External search and share intents now forward only supported data, while app data is explicitly excluded from Android cloud backups and device transfers.
- Upcoming category filters now follow the active profile immediately.

## [1.5.5] - 2026-08-27

### 🧩 Improved

- Statistics activity charts now scroll continuously between adjacent periods, preserve the stopping position, and keep date labels aligned with the displayed data.

## [1.5.4] - 2026-08-26

### 🐛 Fixed

- Statistics current streaks now remain active through an incomplete current day and continue across activity outside the selected date range.

## [1.5.3] - 2026-08-25

### 🐛 Fixed

- Statistics activity charts now keep the first and last activity bars inset from the chart borders for clearer edge spacing.

## [1.5.2] - 2026-08-24

### ✨ Added

- The all-time Statistics view now summarizes precisely recorded time, supports year-to-month drill-down for long histories, and labels chart bars as non-cumulative totals.

### 🧩 Improved

- Activity history sessions now include entry covers, session durations, completion badges, and detailed chapter, episode, or item breakdowns that can be expanded for longer sessions.
- Statistics progress now shows each library status with its count and proportional bar, making the breakdown easier to compare.

## [1.5.1] - 2026-08-24

### ✨ Added

- Added navigation through older and newer activity periods in the Statistics dashboard for 7-day, 30-day, and 1-year ranges, with swipe gestures, previous and next controls, accessibility actions, and a `Today` shortcut.
- Added clear visual treatment for periods before detailed activity tracking began, including `Not tracked` chart regions and period-specific loading and retry feedback.

### 🧩 Improved

- Statistics summary and insight cards now keep equal heights when displayed together.

### 🐛 Fixed

- Translation popups now stay clear of text-selection handles so their actions remain tappable.

## [1.5.0] - 2026-08-23

### 🌟 Highlights

Explore your reading and viewing habits with detailed activity trends, progress, streaks, completions, and media-specific insights in the rebuilt Statistics dashboard.

### ✨ Added

- Added detailed activity statistics for manga, books, and anime, including time spent, sessions, completions, active days, streaks, top titles, and selectable date ranges.
- Added activity drill-downs for individual date ranges and entries, plus a separate view of activity recorded before detailed Statistics tracking began.
- Added library coverage and progress breakdowns by media type, with genre, category, and source insights.

### 🔄 Changed

- Entry backups now preserve and restore detailed activity sessions and completion records used by Statistics.

## [1.4.1] - 2026-08-21

### 🔄 Changed

- Library selection pin actions now reflect the selected entries' current states and are hidden when a selection mixes pinned and unpinned entries.

### 🧩 Improved

- Pinned library display settings now include visual previews, and shelf layouts adapt to available space while showing the next entry when more are available.
- Library grid fast scrolling now tracks actual row sizes, preventing scrollbar jumps.
- Backup restoration now preserves the newer entry's pinned or unpinned state.

## [1.4.0] - 2026-08-20

### 🌟 Highlights

Pin library entries to keep them together at the top of your library, with a choice of tonal-group or horizontal-shelf display.

### ✨ Added

- Added bulk pin and unpin actions for library entries, keeping pinned entries prioritized across library sorting modes.
- Added configurable pinned-entry displays for list and grid layouts, with tonal-group and horizontal-shelf options.

## [1.3.12] - 2026-08-19

### 🐛 Fixed

- Book reader progress indicators now continue through chapter transitions and reach 100% at the chapter's terminal scroll boundary.

## [1.3.11] - 2026-08-16

### 🐛 Fixed

- Viewer settings tabs now remain stable while editing numeric values.

## [1.3.10] - 2026-08-15

### ✨ Added

- Added a book-reader option to keep the screen awake while reading.
- Added a book-reader option to control the standard text-selection menu.

### 🐛 Fixed

- Book reader themes now apply consistently to reader controls, translation popups, and text-selection colors.
- The text-selection menu now stays stable while translation results resize.

## [1.3.9] - 2026-08-15

### ✨ Added

- Added Paper and Dusk themes for the book reader.

### 🧩 Improved

- The book reader table of contents now shows read status, bookmarks, reading progress, and the current chapter.
- Reader controls now use larger touch targets in manga and book readers.

### 🐛 Fixed

- The book reader now shows feedback when an internal section link is missing or an external link cannot be opened.
- Saved per-entry reader settings now apply before reader content is rendered.

## [1.3.8] - 2026-08-15

### 🔄 Changed

- Immersive catalog and feed views now keep the system bars visible instead of hiding them.

## [1.3.7] - 2026-08-14

### ✨ Added

- Added an option to keep the navigation bar visible in the book reader when reader controls are hidden.

## [1.3.6] - 2026-08-14

### 🐛 Fixed

- Book-reader translation popups now remain visible and re-anchor while scrolling without conflicting with text-selection action menus.

## [1.3.5] - 2026-08-14

### 🐛 Fixed

- Book reader progress indicators now reflect the visible scrollable range accurately, preserve the actual resume position independently, and stay within rounded screen corners.

## [1.3.4] - 2026-08-13

### 🐛 Fixed

- Book reader text reflow no longer causes a delayed jump after changing text size.

## [1.3.3] - 2026-08-13

### ✨ Added

- Added adjustable book-reader text size from 80% to 200% in 10% steps, with direct value entry and reading position preservation when text reflows.
- Added configurable book-reading progress, including an option to hide it or choose percentage, edge fill rail, edge position marker, or bottom hairline styles.

### 🧩 Improved

- Immersive video controls now hide automatically after three seconds of active playback and reappear when you interact with playback controls.

## [1.3.2] - 2026-08-13

### 🧩 Improved

- Book downloads are now discovered and reflected in library and update counts progressively, and queued manga or book downloads resume after an app restart unless you explicitly paused them.

### 🐛 Fixed

- Leaving the entry notes editor now preserves the final text edit even when it occurs before the autosave delay completes.
- Translation pickers now size correctly, show a clear no-results message for language searches, and keep compact translation controls aligned.

### ⚡️ Performance

- Improved responsiveness when browsing large libraries, filtering or grouping them, searching merge targets, and loading related entries.

## [1.3.1] - 2026-08-11

### 🐛 Fixed

- Manga and book readers now keep retryable startup failures on screen with a `Retry` action instead of closing immediately.
- Webtoon auto-scroll now stops when you manually scroll, zoom, or drag the reader.
- Library tabs now preserve the selected page separately for each profile when switching profiles.

### 🧩 Improved

- Unread book chapters with saved partial progress now show a percentage read in chapter lists when available.

### ⚡️ Performance

- Improved responsiveness during profile startup and when browsing large libraries or entry chapter lists.

## [1.3.0] - 2026-08-10

### 🌟 Highlights

Listen to selected book passages and translation results with configurable speech engines and voices, while selecting text across whole book chapters for copying, translation, or speech.

### ✨ Added

- Added configurable text-to-speech for selected book passages and translation results, with selectable engines and voices, language-specific voice overrides, pitch controls, playback previews, and network-voice consent.
- Added chapter-wide text selection in the book reader (previously selection was possibly only inside one block).
- Added an optional book-reader status bar setting that keeps the status bar visible while reader controls are hidden.

### 🐛 Fixed

- Opening book-reader settings now preserves the current reading position.
- Translation picker dialogs now adapt their height to their content instead of occupying the full available height.
- Book reader navigation bars now match the reader appearance without an unwanted contrast overlay.

### ⚡️ Performance

- Book chapter transitions no longer interrupt continuous scrolling while adjacent chapters load.

## [1.2.1] - 2026-08-08

### ✨ Added

- Added a `Go to current chapter` action to entry screens that scrolls to the next chapter to
  continue reading and highlights it.

### 🔄 Changed

- Book reader progress stays visible when reader controls are hidden and moves above the controls
  when they are shown.

### ⚡️ Performance

- Improved responsiveness when loading large libraries or entry screens and moving between book
  chapters.

## [1.2.0] - 2026-08-08

### 🌟 Highlights

See supported manga images take shape while they download, with smoother book chapter transitions.

### ✨ Added

- Added optional progressive image loading for supported manga images, showing downloaded portions in
  manga readers and entry previews before the full image is available, including animated previews
  where supported. Experimental and available under Settings -> Advanced -> Progressive image loading.

### ⚡️ Performance

- Removed unnecessary work during Book chapter transition that caused noticeable lag
  for entries with large amount of chapters.

Based on [Mihon 0.20.4](https://github.com/mihonapp/mihon/releases/tag/v0.20.4)

## [1.1.4] - 2026-08-04

### 🐛 Fixed

- Restored the selected grouped library page after relaunch and kept grouped library pages isolated
  when switching profiles.

Based on [Mihon 0.20.3](https://github.com/mihonapp/mihon/releases/tag/v0.20.3)

## [1.1.3] - 2026-08-04

### ⚡ Improved

- Prefetch the first viewport of the next book chapter for smoother chapter transitions.

### 🐛 Fixed

- Restored tracking data correctly from backups.

## [1.1.2] - 2026-08-03

### 🌟 Highlights

Customize library grouping levels while navigating immersive manga and book readers more reliably.

### ✨ Added

- Configure the library grouping hierarchy by enabling and reordering category, entry type, and
  source levels, or view all entries without grouping.

### 🐛 Fixed

- Restored page navigation in immersive manga browsing while an image is zoomed in.
- Prevented chapter transition controls in the book reader from triggering reader tap actions.

## [1.1.1] - 2026-08-02

### 🌟 Highlights

Jump directly to any page while browsing manga in immersive mode with the new page scrubber.

### ✨ Added

- Added a bottom page scrubber to immersive manga browsing for quick navigation with haptic
  feedback while scrubbing.

## [1.1.0] - 2026-08-02

### 🌟 Highlights

Review and resume source migrations while replacement searches run in the background, with clearer
immersive-media loading and more reliable downloads.

### ✨ Added

- Source migration can search replacement entries in the background, with review filters,
  per-entry replacement selection, conflict handling, pause and resume controls, and progress
  notifications.

### ⚡ Improved

- Immersive manga and anime browsing now provides clearer loading and retry feedback, with preview
  backgrounds and download progress for manga pages while they load.

### 🐛 Fixed

- Grouped merged entries into a single merge target instead of listing each member separately.
- Queued BOOK downloads in reading order.
- Cleared selected chapters after they are queued for download.

[Unreleased]: https://github.com/pa2x2/katari/compare/v2.0.0...upcoming
[2.0.0]: https://github.com/pa2x2/katari/releases/tag/v2.0.0
[1.17.1]: https://github.com/pa2x2/katari/releases/tag/v1.17.1
[1.17.0]: https://github.com/pa2x2/katari/releases/tag/v1.17.0
[1.16.4]: https://github.com/pa2x2/katari/releases/tag/v1.16.4
[1.16.3]: https://github.com/pa2x2/katari/releases/tag/v1.16.3
[1.16.2]: https://github.com/pa2x2/katari/releases/tag/v1.16.2
[1.16.1]: https://github.com/pa2x2/katari/releases/tag/v1.16.1
[1.16.0]: https://github.com/pa2x2/katari/releases/tag/v1.16.0
[1.15.1]: https://github.com/pa2x2/katari/releases/tag/v1.15.1
[1.15.0]: https://github.com/pa2x2/katari/releases/tag/v1.15.0
[1.14.2]: https://github.com/pa2x2/katari/releases/tag/v1.14.2
[1.14.1]: https://github.com/pa2x2/katari/releases/tag/v1.14.1
[1.14.0]: https://github.com/pa2x2/katari/releases/tag/v1.14.0
[1.13.3]: https://github.com/pa2x2/katari/releases/tag/v1.13.3
[1.13.2]: https://github.com/pa2x2/katari/releases/tag/v1.13.2
[1.13.1]: https://github.com/pa2x2/katari/releases/tag/v1.13.1
[1.13.0]: https://github.com/pa2x2/katari/releases/tag/v1.13.0
[1.12.0]: https://github.com/pa2x2/katari/releases/tag/v1.12.0
[1.11.0]: https://github.com/pa2x2/katari/releases/tag/v1.11.0
[1.10.2]: https://github.com/pa2x2/katari/releases/tag/v1.10.2
[1.10.1]: https://github.com/pa2x2/katari/releases/tag/v1.10.1
[1.10.0]: https://github.com/pa2x2/katari/releases/tag/v1.10.0
[1.9.0]: https://github.com/pa2x2/katari/releases/tag/v1.9.0
[1.8.6]: https://github.com/pa2x2/katari/releases/tag/v1.8.6
[1.8.5]: https://github.com/pa2x2/katari/releases/tag/v1.8.5
[1.8.4]: https://github.com/pa2x2/katari/releases/tag/v1.8.4
[1.8.3]: https://github.com/pa2x2/katari/releases/tag/v1.8.3
[1.8.2]: https://github.com/pa2x2/katari/releases/tag/v1.8.2
[1.8.1]: https://github.com/pa2x2/katari/releases/tag/v1.8.1
[1.8.0]: https://github.com/pa2x2/katari/releases/tag/v1.8.0
[1.7.0]: https://github.com/pa2x2/katari/releases/tag/v1.7.0
[1.6.0]: https://github.com/pa2x2/katari/releases/tag/v1.6.0
[1.5.5]: https://github.com/pa2x2/katari/releases/tag/v1.5.5
[1.5.4]: https://github.com/pa2x2/katari/releases/tag/v1.5.4
[1.5.3]: https://github.com/pa2x2/katari/releases/tag/v1.5.3
[1.5.2]: https://github.com/pa2x2/katari/releases/tag/v1.5.2
[1.5.1]: https://github.com/pa2x2/katari/releases/tag/v1.5.1
[1.5.0]: https://github.com/pa2x2/katari/releases/tag/v1.5.0
[1.4.1]: https://github.com/pa2x2/katari/releases/tag/v1.4.1
[1.4.0]: https://github.com/pa2x2/katari/releases/tag/v1.4.0
[1.3.12]: https://github.com/pa2x2/katari/releases/tag/v1.3.12
[1.3.11]: https://github.com/pa2x2/katari/releases/tag/v1.3.11
[1.3.10]: https://github.com/pa2x2/katari/releases/tag/v1.3.10
[1.3.9]: https://github.com/pa2x2/katari/releases/tag/v1.3.9
[1.3.8]: https://github.com/pa2x2/katari/releases/tag/v1.3.8
[1.3.7]: https://github.com/pa2x2/katari/releases/tag/v1.3.7
[1.3.6]: https://github.com/pa2x2/katari/releases/tag/v1.3.6
[1.3.5]: https://github.com/pa2x2/katari/releases/tag/v1.3.5
[1.3.4]: https://github.com/pa2x2/katari/releases/tag/v1.3.4
[1.3.3]: https://github.com/pa2x2/katari/releases/tag/v1.3.3
[1.3.2]: https://github.com/pa2x2/katari/releases/tag/v1.3.2
[1.3.1]: https://github.com/pa2x2/katari/releases/tag/v1.3.1
[1.3.0]: https://github.com/pa2x2/katari/releases/tag/v1.3.0
[1.2.1]: https://github.com/pa2x2/katari/releases/tag/v1.2.1
[1.2.0]: https://github.com/pa2x2/katari/releases/tag/v1.2.0
[1.1.4]: https://github.com/pa2x2/katari/releases/tag/v1.1.4
[1.1.3]: https://github.com/pa2x2/katari/releases/tag/v1.1.3
[1.1.2]: https://github.com/pa2x2/katari/releases/tag/v1.1.2
[1.1.1]: https://github.com/pa2x2/katari/releases/tag/v1.1.1
[1.1.0]: https://github.com/pa2x2/katari/releases/tag/v1.1.0
[1.0.0]: https://github.com/pa2x2/katari/releases/tag/v1.0.0
