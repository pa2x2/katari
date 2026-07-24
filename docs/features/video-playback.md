# Video playback

Entries with playable video open their child items in Katari's video player. The source resolves the requested item into streams and may also expose audio variants, source/server options, qualities, and subtitles.

## Choose playback options

Available controls depend on the source. Sources define the labels and exact meaning of their Dub, Source quality, and Stream choices; the descriptions below are common conventions:

- **Dub** selects an audio or release variant.
- **Source quality** selects a provider or server variant.
- **Stream** selects one of the source's stream descriptors.
- **Playback quality** selects automatic adaptation or a preferred video height within the chosen stream.
- **Subtitles** selects Off or an available embedded or external track. Default and forced metadata can influence which track Katari initially selects.
- **Playback speed** changes the current playback rate.

Sources can fall back when a requested option disappears. Katari records the option actually returned so later playback starts from a valid selection.

## Player controls

The player exposes its currently available playback and subtitle controls while an item is playing. Use **Reset** in the player settings sheet to restore automatic source and quality selection, normal playback speed, and the default subtitle selection. Persistent player preferences are available under **More → Settings → Player**.

## Resume progress

Katari stores playback position and completion state per profile and child item. Opening the same item can resume from its saved position. Entry types presented as watched content use watched terminology while sharing Katari's common history surface.

## Playback failures

If playback fails:

1. Try another stream, quality, dub, or source option.
2. During an in-playback failure, use **Retry** so the source can resolve playback again while preserving the current position. Retrying an initial resolution failure has no playback position to preserve.
3. Check whether the source works in its WebView or catalogue.
4. Report the source, episode, selected options, and error without sharing signed media URLs or authentication headers.
