package eu.kanade.presentation.more.stats.recap.motion

import mihon.core.platform.PlatformContext

/** Whether the system has animations removed, which stops recap motion with entrances finished. */
internal expect fun PlatformContext.recapAnimationsRemoved(): Boolean

/**
 * 1, except in builds that turn on `recap_follows_animator_scale`, where the story stretches with the system animator
 * scale along with the Compose animations around it. The showcase captures with such a build: a device too slow to draw
 * the story at full rate records it slowed down, each frame then covering less of the story, and speeds the clip up
 * again.
 */
internal expect fun PlatformContext.recapSlowdown(): Float
