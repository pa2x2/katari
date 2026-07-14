# Feeds and discovery

Katari extends source browsing with reusable feeds. A feed binds one source to a listing preset and keeps that discovery view available from the **Feeds** tab.

## Add a feed

Use the add action in **Browse → Feeds**, select a source, and then choose a preset. Sources can provide presets, while Katari also recognizes built-in **Popular** and **Latest** listings. A preset can represent a saved search with its filter state.

If no feed is configured, the Feeds screen shows an empty-state message. Use the add action in the app bar to create one.

## Manage feeds

Open feed management to:

- Enable or disable feeds without deleting them
- Reorder feeds
- Remove feeds

In the regular feed view, use the navigation bar to move between the previous and next enabled feed and to select that feed's display mode. Tap the current feed to open the feed picker, jump directly to any enabled feed, or open the add and management actions.

Feed configuration and scroll state are profile-specific.

## Regular and chronological views

Regular mode uses familiar catalogue grid or list layouts. Every feed preserves its timeline and position. When a chronological feed refreshes, it checks the newest source page first, then fills any gap from the saved timeline in the background. The saved timeline remains available while a loading boundary at its top prevents partially loaded pages from appearing out of chronological order. Once the gap is connected, all newer results are prepended without moving the visible item and you can scroll through them continuously. The new-items chip shows progress and remains an immediate shortcut to the newest result. If the source cannot bridge the gap within a bounded number of pages, the chip still offers a user-controlled switch to the newest results, which then load older pages normally as you continue browsing. Pagination depends on the source and preset; a feed cannot produce more results after its source reports the end.

## Immersive browsing

When the source advertises immersive-feed support, switch from regular to immersive mode for a media-forward, full-screen discovery experience. You can enter immersive mode from either the source catalogue or one of that source's feeds; sources that do not opt in do not expose the control. In immersive mode you can move through items, add entries to the library, open their details or media, and return to regular browsing. Pull down from the first item to refresh the current catalogue or feed. The feed view also keeps its feed picker and chronological-feed actions available.

Under **Settings → Browse → Behavior**, reorder the default long-press actions used by source catalogues and feeds, then add overrides for sources that should behave differently. You can also open a source's override directly from its catalogue menu or feed picker. Katari tries actions from top to bottom. **Start immersive view** opens immersive browsing at the pressed item when the source supports it; otherwise Katari continues to the next action. Preview is skipped in the same way when it is unavailable, while the library action is always available as a fallback. Defaults and source overrides belong to the active profile.

Preview media comes from the source. Missing or failing previews do not necessarily mean the entry itself cannot be opened.

## Global search

Global search still searches across enabled sources and can be restricted by pinned-source behavior. Results preserve their entry type, allowing different kinds of entries to appear in the same search surface while opening with the correct interaction.
