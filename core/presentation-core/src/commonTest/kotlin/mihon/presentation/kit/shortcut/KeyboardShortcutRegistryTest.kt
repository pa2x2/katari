package mihon.presentation.kit.shortcut

import androidx.compose.ui.input.key.Key
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KeyboardShortcutRegistryTest {
    @Test
    fun `a key runs the newest declaration it reaches, never one under an overlay or one a text field keeps`() {
        val registry = KeyboardShortcutRegistry()
        val ran = mutableListOf<String>()
        fun declare(name: String, shortcut: KeyboardShortcut) =
            ShortcutRegistration(registry.rootLayer, shortcut, enabled = true, action = { ran += name })
                .also(registry::register)

        declare("leaving screen", refresh)
        declare("shown screen", refresh)
        declare("window", quit)
        registry.trigger(KeyPress(Key.F5)) shouldBe true
        // The screen being left is still composed during the transition but no longer gets its keys
        ran shouldBe listOf("shown screen")

        val dialog = ShortcutLayer()
        registry.open(dialog)
        ran.clear()
        registry.trigger(KeyPress(Key.F5)) shouldBe false
        registry.trigger(KeyPress(Key.Q, ctrl = true)) shouldBe true
        ran shouldBe listOf("window")

        registry.close(dialog)
        val searchField = Any()
        registry.typingStarted(searchField)
        declare("search", search)
        ran.clear()
        // Ctrl+F reaches the window only because the focused field has no use for it
        registry.trigger(KeyPress(Key.F, ctrl = true)) shouldBe false
        registry.trigger(KeyPress(Key.F5)) shouldBe true
        ran shouldBe listOf("shown screen")
    }

    private val refresh = shortcut("refresh", ShortcutKeys.Chord(Key.F5, "F5"))
    private val quit = shortcut("quit", ShortcutKeys.Chord(Key.Q, "Q", ctrl = true), ShortcutReach.Window)
    private val search = shortcut("search", ShortcutKeys.Chord(Key.F, "F", ctrl = true), whileTyping = false)

    private fun shortcut(
        id: String,
        keys: ShortcutKeys,
        reach: ShortcutReach = ShortcutReach.Screen,
        whileTyping: Boolean = true,
    ) = KeyboardShortcut(id, id, listOf(keys), ShortcutGroup.CurrentScreen, reach, whileTyping)
}
