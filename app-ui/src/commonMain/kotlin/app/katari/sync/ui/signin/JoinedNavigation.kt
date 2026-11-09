package app.katari.sync.ui.signin

import app.katari.sync.ui.firstsync.FirstSyncScreen
import cafe.adriel.voyager.navigator.Navigator

/** A screen that opens signing in or linking, such as the Sync page or the first-run guide; joining returns to it. */
interface SyncJoinOrigin

/**
 * Leaves the screens of signing in or linking for the screen they were opened from, and goes on to the first-sync
 * choices when the device has them to make, so joining never stops short of them (D027).
 */
internal fun Navigator.leaveJoined(joined: SyncJoinViewModel.JoinState.Joined) {
    popUntil { it is SyncJoinOrigin }
    if (joined.choicesWaiting) push(FirstSyncScreen())
}
