package tachiyomi.domain.manga.service

import android.content.Context
import android.content.SharedPreferences
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions

class DuplicatePreferencesTest {

    @Test
    fun `title exclusions default to bracket patterns`() {
        val preferences = DuplicatePreferences(InMemoryPreferenceStore())

        preferences.titleExclusionPatterns.get() shouldBe DuplicateTitleExclusions.defaultPatterns
    }

    @Test
    fun `title exclusions sanitize duplicates and catch all patterns`() {
        val sharedPreferences = FakeSharedPreferences().apply {
            edit()
                .putString(
                    "extended_duplicate_detection_title_exclusion_patterns",
                    "[\"[*]\",\"**\",\"(*)\",\"[*]\"]",
                )
                .commit()
        }
        val store = AndroidPreferenceStore(
            context = mockk<Context>(relaxed = true),
            sharedPreferences = sharedPreferences,
        )

        val preferences = DuplicatePreferences(store)

        preferences.titleExclusionPatterns.get() shouldBe listOf("[*]", "(*)")
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val data = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = LinkedHashMap(data)

        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            val value = data[key] as? Set<String>
            return value?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        private inner class Editor : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                data[key.orEmpty()] = value
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                data[key.orEmpty()] = values?.toSet()
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                data[key.orEmpty()] = value
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                data[key.orEmpty()] = value
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                data[key.orEmpty()] = value
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                data[key.orEmpty()] = value
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                data.remove(key)
            }

            override fun clear(): SharedPreferences.Editor = apply {
                data.clear()
            }

            override fun commit(): Boolean = true

            override fun apply() = Unit
        }
    }
}
