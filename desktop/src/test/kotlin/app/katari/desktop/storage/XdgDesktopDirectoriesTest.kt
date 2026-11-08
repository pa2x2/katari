package app.katari.desktop.storage

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class XdgDesktopDirectoriesTest {

    @TempDir
    lateinit var home: Path

    @Test
    fun `empty and relative variables fall back to the home directory instead of the working directory`() {
        val directories = XdgDesktopDirectories(
            "katari",
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

    @Test
    fun `pictures come from user-dirs in either form, and a folder that is the home directory counts as none`() {
        val configuration = home.resolve(".config").createDirectories()
        fun picturesFor(line: String): Path? {
            configuration.resolve("user-dirs.dirs").writeText(
                "# This file is written by xdg-user-dirs-update\nXDG_DESKTOP_DIR=\"\$HOME/Desktop\"\n$line\n",
            )
            return XdgDesktopDirectories("katari", mapOf("HOME" to home.toString())).pictures
        }

        picturesFor("XDG_PICTURES_DIR=\"\$HOME/Bilder\"") shouldBe home.resolve("Bilder")
        picturesFor("XDG_PICTURES_DIR=\"/media/photos\"") shouldBe Path("/media/photos")
        picturesFor("XDG_PICTURES_DIR=\"\$HOME/\"") shouldBe null
    }
}
