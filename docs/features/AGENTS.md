# Feature Documentation Notes

## Content Type Matrix
- Keep `content-type-reference.md` aligned with the capabilities implemented or explicitly enabled by each content type.
- A capability belongs in the matrix when every content type must implement, configure, or opt into it. Rows where all current types are supported are valid when support comes from separate type implementations.
- Do not add functionality supplied automatically by the shared `Entry` infrastructure, such as unified-library or global-search participation.
- Do not compare media-native controls, such as reader gestures, video streams, audio tracks, or subtitles; document those in the relevant media experience instead.
- Use the source-dependent state when the content-type implementation exists but availability requires a source capability.
- When the matrix may need updating, audit the registered processors in `entry-interactions/<type>` and type-gated integrations such as local sources and trackers.
