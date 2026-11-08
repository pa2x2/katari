package mihon.desktop.backup

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import mihon.desktop.profile.DesktopProfileStore
import mihon.desktop.storage.TemporaryDesktopDirectories
import mihon.feature.profiles.core.ProfileConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.Preference
import java.nio.file.Path

class DesktopPreferenceBackupStoresTest {

    @TempDir
    lateinit var directory: Path

    // Desktop reads a profile's private preferences from its private store and its other settings from its profile
    // store, where Android has one store for both. Restoring either into the other store would drop it without a trace,
    // and restoring it into another profile's store would hand it to that profile.
    @Test
    fun `a profile's private and other settings restore into the stores desktop reads them from`() {
        val profileId = 2L
        val privateKey = Preference.privateKey("account_token")
        val backedUpDirectories = TemporaryDesktopDirectories(directory.resolve("backed-up"))
        val backedUp = DesktopProfileStore(backedUpDirectories)
        backedUp.privateStore(profileId).getString(privateKey).set("token")
        backedUp.profileStore(profileId).getString("theme").set("dark")
        val preferences = DesktopPreferenceBackupStores(backedUp).profilePreferences(profileId)

        val restoredDirectories = TemporaryDesktopDirectories(directory.resolve("restored"))
        val restored = DesktopProfileStore(restoredDirectories)
        val stores = DesktopPreferenceBackupStores(restored)
        preferences.forEach { (key, value) ->
            stores.profileStoreFor(profileId, key).getString(key).set(value as String)
        }

        restored.privateStore(profileId).getString(privateKey).get() shouldBe "token"
        restored.profileStore(profileId).getString("theme").get() shouldBe "dark"
        stores.profilePreferences(ProfileConstants.DEFAULT_PROFILE_ID).shouldBeEmpty()

        backedUpDirectories.awaitWritten("profiles/$profileId/profile.json", "profiles/$profileId/private.json")
        restoredDirectories.awaitWritten("profiles/$profileId/profile.json", "profiles/$profileId/private.json")
    }
}
