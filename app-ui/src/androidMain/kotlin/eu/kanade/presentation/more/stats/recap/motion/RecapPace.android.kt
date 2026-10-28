package eu.kanade.presentation.more.stats.recap.motion

import eu.kanade.tachiyomi.util.system.animatorDurationScale
import mihon.app.ui.R
import mihon.core.platform.PlatformContext

internal actual fun PlatformContext.recapAnimationsRemoved(): Boolean = animatorDurationScale == 0f

internal actual fun PlatformContext.recapSlowdown(): Float {
    if (!resources.getBoolean(R.bool.recap_follows_animator_scale)) return 1f
    return animatorDurationScale.takeIf { it > 0f } ?: 1f
}
