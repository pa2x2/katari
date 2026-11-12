package app.katari.desktop.autostart

import io.kotest.matchers.collections.shouldContainAll
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AutostartEntryTest {

    // A wrongly quoted path only shows at the next login, as the app silently not starting
    @Test
    fun `a program path with spaces, quotes, dollars, backslashes and percent signs reads back whole`() {
        val text = AutostartEntry.text(Path.of("/home/a b/K\"a\$t\\i%.AppImage"), name = "Katari", icon = "app.katari")

        text.lines() shouldContainAll listOf(
            """Exec="/home/a b/K\\"a\\${'$'}t\\\\i%%.AppImage" --start-in-tray""",
            """TryExec=/home/a b/K"a${'$'}t\\i%.AppImage""",
        )
    }
}
