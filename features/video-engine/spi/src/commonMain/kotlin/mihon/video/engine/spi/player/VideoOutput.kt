package mihon.video.engine.spi.player

/**
 * Where a player draws its picture: a view, surface or drawing of the platform's UI. Each platform's UI makes the
 * outputs its engines draw into, and hands one to a player with [VideoPlayer.setVideoOutput]; an engine leaves the
 * picture of an output it can't draw into undrawn.
 */
interface VideoOutput
