package app.katari.sync.ui.signin

import app.katari.sync.ui.firstsync.FirstSyncScreen
import app.katari.sync.ui.page.SyncScreen
import cafe.adriel.voyager.navigator.Navigator

/**
 * Leaves the screens of signing in or linking for the Sync page they were opened from, and goes on to the first-sync
 * choices when the device has them to make, so joining never stops short of them (D027).
 */
internal fun Navigator.leaveJoined(joined: SyncJoinViewModel.JoinState.Joined) {
    popUntil { it == SyncScreen }
    if (joined.choicesWaiting) push(FirstSyncScreen)
}
