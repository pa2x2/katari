package mihon.desktop.profile

import io.kotest.matchers.shouldBe
import mihon.desktop.storage.TemporaryDesktopDirectories
import mihon.feature.profiles.core.ProfileConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists

class DesktopProfileStoreTest {

    @TempDir
    lateinit var directory: Path

    // Android opens a new view of its one preference file on every call. Opening a new store per call here would put two
    // stores on one file, and whichever wrote last would drop the other's values.
    @Test
    fun `a profile's stores are the same addressed as the current profile or by ID, and none is the base store`() {
        val directories = TemporaryDesktopDirectories(directory)
        val store = DesktopProfileStore(directories)
        store.profileStore().getString("key").set("profile")
        store.appStateStore().getString("key").set("app state")
        store.privateStore().getString("key").set("private")

        val profileId = ProfileConstants.DEFAULT_PROFILE_ID
        store.profileStore(profileId).getString("key").get() shouldBe "profile"
        store.appStateStore(profileId).getString("key").get() shouldBe "app state"
        store.privateStore(profileId).getString("key").get() shouldBe "private"
        store.basePreferenceStore().getString("key").get() shouldBe ""

        directories.awaitWritten(
            "profiles/$profileId/profile.json",
            "profiles/$profileId/app-state.json",
            "profiles/$profileId/private.json",
        )
    }

    // Preference classes take their store once, before any switch, and profile IDs are reused once the highest is
    // deleted. A store fixed to the profile active when it was taken would keep writing the profile that was left, and
    // a deleted profile's file would hand its values to the next profile given its ID.
    @Test
    fun `settings follow a profile switch, and a deleted profile's settings don't return with its ID`() {
        val directories = TemporaryDesktopDirectories(directory)
        val store = DesktopProfileStore(directories)
        val theme = store.profileStore().getString("theme")
        theme.set("dark")

        store.setCurrentProfileId(2L)
        theme.get() shouldBe ""
        theme.set("light")
        store.profileStore(ProfileConstants.DEFAULT_PROFILE_ID).getString("theme").get() shouldBe "dark"

        store.deleteProfileState(2L)
        theme.get() shouldBe ""
        directory.resolve("profiles/2").exists() shouldBe false

        directories.awaitWritten("base.json", "profiles/${ProfileConstants.DEFAULT_PROFILE_ID}/profile.json")
    }
}
