package mihon.desktop.profile

import io.kotest.matchers.shouldBe
import mihon.desktop.storage.DesktopDirectories
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
        val store = DesktopProfileStore(TestDirectories(directory))
        store.profileStore().getString("key").set("profile")
        store.appStateStore().getString("key").set("app state")
        store.privateStore().getString("key").set("private")

        val profileId = ProfileConstants.DEFAULT_PROFILE_ID
        store.profileStore(profileId).getString("key").get() shouldBe "profile"
        store.appStateStore(profileId).getString("key").get() shouldBe "app state"
        store.privateStore(profileId).getString("key").get() shouldBe "private"
        store.basePreferenceStore().getString("key").get() shouldBe ""

        awaitWritten(
            directory.resolve("profiles/$profileId/profile.json"),
            directory.resolve("profiles/$profileId/app-state.json"),
            directory.resolve("profiles/$profileId/private.json"),
        )
    }

    // The stores write in the background; the temporary directory can only be deleted once they have.
    private fun awaitWritten(vararg files: Path) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!files.all(Path::exists)) {
            check(System.nanoTime() < deadline) { "Not written: ${files.filterNot(Path::exists)}" }
            Thread.sleep(10)
        }
    }
}

private class TestDirectories(directory: Path) : DesktopDirectories {
    override val data: Path = directory
    override val configuration: Path = directory
    override val cache: Path = directory
}
