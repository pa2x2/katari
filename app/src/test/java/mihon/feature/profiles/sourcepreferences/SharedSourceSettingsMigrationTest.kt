package mihon.feature.profiles.sourcepreferences

import io.kotest.matchers.shouldBe
import mihon.feature.profiles.FakeSharedPreferences
import org.junit.jupiter.api.Test

class SharedSourceSettingsMigrationTest {

    // The older profile migration deleted the file extensions read, so both that file and the copy it left hold settings
    @Test
    fun `every profile gets the copy with the shared file on top, and redoing it changes nothing`() {
        val shared = mapOf("mirror" to "0", "full_title" to false)
        val defaultProfile = FakeSharedPreferences().apply {
            edit().putString("mirror", "2").putString("slug_map", "{}").commit()
        }
        val otherProfile = FakeSharedPreferences().apply {
            edit().putString("mirror", "5").putString("restored", "x").commit()
        }
        val expectedDefault = mapOf("mirror" to "0", "slug_map" to "{}", "full_title" to false)

        repeat(2) {
            copySharedSourceSettings(shared, defaultProfile, listOf(defaultProfile, otherProfile))

            defaultProfile.all shouldBe expectedDefault
            otherProfile.all shouldBe expectedDefault + ("restored" to "x")
        }
    }
}
