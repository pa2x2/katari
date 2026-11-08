package app.katari.entry.interactions.media

import app.katari.entry.interactions.media.session.EntryMediaSessionEventSink

/**
 * Operational type-owned media-session seam.
 *
 * Media runtimes use the same instance they contribute. Its presence is the authoritative fact that the type emits
 * structured media-session events and therefore participates in every applicable shared consequence.
 */
interface EntryMediaSessionProcessor : EntryMediaSessionEventSink
