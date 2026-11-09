package app.katari.gradle.preferencescope

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PreferenceScopeRulesTest {

    @Test
    fun `a preference key no table marks is reported, however the class names it`() {
        val preferences = """
            import tachiyomi.core.common.preference.PreferenceStore

            class ReaderPreferences(private val store: PreferenceStore) {
                val theme = store.getString("reader_theme", "dark")
                val brightness = store.getInt(BRIGHTNESS_KEY, 0)
                val lastPage = store.getLong(Preference.appStateKey("last_page"), 0)
                fun pinned(id: Long) = store.getBoolean(PINNED_FAMILY.key(id), false)
                fun zoom(surface: String) = store.getFloat("${'$'}ZOOM_PREFIX${'$'}surface", 1f)

                companion object {
                    const val BRIGHTNESS_KEY = "reader_brightness"
                    const val ZOOM_PREFIX = "reader_zoom:"
                    val PINNED_FAMILY = ProfilePreferenceKeyPattern.Prefix("reader_pinned_")
                }
            }
        """.trimIndent()
        val table = """
            internal val ReaderScopes = PreferenceScopeTable(SettingsArea.READERS) {
                person("reader_theme")
                devicePrefix(DeviceBound.HARDWARE, "reader_zoom:")
            }
        """.trimIndent()

        val findings = checkPreferenceScopes(
            listOf(
                PreferenceScopeSource("reader/src/commonMain/kotlin/ReaderPreferences.kt", preferences),
                PreferenceScopeSource("features/sync/client/src/commonMain/kotlin/ReaderScopes.kt", table),
            ),
        )

        assertEquals(
            listOf(
                "reader/src/commonMain/kotlin/ReaderPreferences.kt:5: \"reader_brightness\"",
                "reader/src/commonMain/kotlin/ReaderPreferences.kt:7: \"reader_pinned_\"",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber}: ${it.reason.substringAfter("key").quoted()}" },
        )
    }

    @Test
    fun `a mark no preference uses is reported`() {
        val preferences = """
            class LibraryPreferences(private val store: PreferenceStore) {
                val sort = store.getString("library_sort", "")
            }
        """.trimIndent()
        val table = """
            internal val LibraryScopes = PreferenceScopeTable {
                person(
                    "library_sort",
                    "library_removed_setting",
                )
            }
        """.trimIndent()

        val findings = checkPreferenceScopes(
            listOf(
                PreferenceScopeSource("core/domain/src/commonMain/kotlin/LibraryPreferences.kt", preferences),
                PreferenceScopeSource("features/sync/client/src/main/kotlin/LibraryScopes.kt", table),
            ),
        )

        assertEquals(
            listOf(
                "features/sync/client/src/main/kotlin/LibraryScopes.kt:2: " +
                    "no preference uses \"library_removed_setting\"",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber}: ${it.reason.substringBefore(';')}" },
        )
    }

    @Test
    fun `a key the source can't tell is reported`() {
        val preferences = """
            class EntryPreferences(private val store: PreferenceStore) {
                fun note(entry: Entry) = store.getString(entry.id.toString(), "")
            }
        """.trimIndent()

        val findings = checkPreferenceScopes(
            listOf(PreferenceScopeSource("core/domain/src/commonMain/kotlin/EntryPreferences.kt", preferences)),
        )

        assertEquals(
            listOf("core/domain/src/commonMain/kotlin/EntryPreferences.kt:2"),
            findings.map { "${it.relativePath}:${it.lineNumber}" },
        )
    }

    private fun String.quoted(): String = "\"" + substringAfter('"').substringBefore('"') + "\""
}
