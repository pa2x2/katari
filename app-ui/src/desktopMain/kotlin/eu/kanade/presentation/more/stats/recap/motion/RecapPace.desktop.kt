package eu.kanade.presentation.more.stats.recap.motion

import mihon.core.platform.PlatformContext

// Desktop has no system animation scale, and no build records the story slowed down.
internal actual fun PlatformContext.recapAnimationsRemoved(): Boolean = false

internal actual fun PlatformContext.recapSlowdown(): Float = 1f
