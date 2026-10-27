package mihon.core.platform

import androidx.compose.runtime.Composable

/**
 * Where a call that opens something comes from. Android: the caller's `Context`, so a screen opened from an activity
 * joins its task with its transitions. Desktop: the window the call comes from.
 */
expect abstract class PlatformContext

@Composable
expect fun currentPlatformContext(): PlatformContext
