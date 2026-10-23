package mihon.feature.profiles.sourcepreferences

import android.content.SharedPreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mihon.feature.profiles.FakeSharedPreferences
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.SourcePreferenceProfile

class ProfileResolvingSharedPreferencesTest {

    private val files = mapOf(1L to settings("a"), 2L to settings("b"), 3L to settings("c"))
    private var activeProfileId = 1L
    private val preferences = ProfileResolvingSharedPreferences(
        open = files::getValue,
        activeProfileId = { activeProfileId },
    )

    // Extensions keep the object for the source's lifetime, so a file chosen once would hand one profile's login to all
    @Test
    fun `a held object reads and writes the file of the profile each call works for`() = runTest {
        preferences.getString(TOKEN, null) shouldBe "a"

        activeProfileId = 2L
        preferences.getString(TOKEN, null) shouldBe "b"

        withContext(SourcePreferenceProfile.of(3L)) {
            preferences.getString(TOKEN, null) shouldBe "c"
            preferences.edit().putString(TOKEN, "c2").commit()
        }
        files.getValue(3L).getString(TOKEN, null) shouldBe "c2"
        files.getValue(2L).getString(TOKEN, null) shouldBe "b"
    }

    // A listener left on the old profile's file would miss every later change the extension can see
    @Test
    fun `listeners move to the new active profile and hear what the switch changed`() {
        files.getValue(1L).edit().putString(MIRROR, "1").commit()
        files.getValue(2L).edit().putString(MIRROR, "1").commit()
        val heard = mutableListOf<String?>()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> heard += key }
        preferences.registerOnSharedPreferenceChangeListener(listener)

        activeProfileId = 2L
        preferences.onActiveProfileChanged(2L)
        files.getValue(1L).edit().putString(MIRROR, "2").commit()
        files.getValue(2L).edit().putString(MIRROR, "3").commit()

        heard shouldBe listOf(TOKEN, MIRROR)
    }

    private fun settings(token: String) = FakeSharedPreferences().apply { edit().putString(TOKEN, token).commit() }

    private companion object {
        const val TOKEN = "token"
        const val MIRROR = "mirror"
    }
}
