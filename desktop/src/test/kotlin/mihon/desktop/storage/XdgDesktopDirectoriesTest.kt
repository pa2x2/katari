package mihon.desktop.storage

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class XdgDesktopDirectoriesTest {

    @TempDir
    lateinit var home: Path

    @Test
    fun `empty and relative variables fall back to the home directory instead of the working directory`() {
        val directories = XdgDesktopDirectories(
            mapOf(
                "HOME" to home.toString(),
                "XDG_DATA_HOME" to "",
                "XDG_CONFIG_HOME" to "relative/config",
            ),
        )

        directories.data shouldBe home.resolve(".local/share/katari")
        directories.configuration shouldBe home.resolve(".config/katari")
        directories.cache shouldBe home.resolve(".cache/katari")
        directories.state shouldBe home.resolve(".local/state/katari")
    }
}
