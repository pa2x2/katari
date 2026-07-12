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

In the regular feed view, use the navigation bar to move between the previous and next enabled feed and to select that feed's display mode.

Feed configuration and scroll state are profile-specific.

## Regular and chronological views

Regular mode uses familiar catalogue grid or list layouts. Every feed preserves its timeline and position. When a chronological feed refreshes, it checks the newest source page without downloading the entire missed backlog. New results that overlap the saved timeline are prepended in place. If the saved timeline is older, the interface offers a user-controlled switch to the newest results, which then load older pages normally as you continue browsing. Pagination depends on the source and preset; a feed cannot produce more results after its source reports the end.

## Immersive feeds

When the source advertises immersive-feed support, switch from regular to immersive mode for a media-forward, full-screen discovery experience. The control is disabled for sources that do not support it. In immersive mode you can open the feed picker, move through items, add entries to the library, and return to regular browsing.

Preview media comes from the source. Missing or failing previews do not necessarily mean the entry itself cannot be opened.

## Global search

Global search still searches across enabled sources and can be restricted by pinned-source behavior. Results preserve their entry type, allowing different kinds of entries to appear in the same search surface while opening with the correct interaction.
