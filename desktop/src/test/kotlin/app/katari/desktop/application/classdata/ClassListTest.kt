package app.katari.desktop.application.classdata

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class ClassListTest {

    @TempDir
    lateinit var directory: Path

    // The JVM refuses to build from a list in which two classes share a number or a class names a number nobody has,
    // and skips a directive read before the class it names: keeping each run's numbers or extensions' entries would
    // fail the build, and a directive put ahead of its class would leave out what it links
    @Test
    fun `the lists of several runs merge into one the JVM can build from`() {
        val first = ClassList.parse(
            lines(
                "# NOTE: Do not modify this file.",
                "java/lang/Object id: 0",
                "app/A id: 1",
                "extension/Source id: 2 super: 0 source: /cache/extension.jar",
                "@lambda-proxy app/A run ()V",
            ),
        )
        // A run still recording, its last line written in part
        val second = ClassList.parse(
            lines(
                "java/lang/Object id: 0",
                "app/B id: 1",
                "app/A id: 2",
                "@lambda-proxy app/A run ()V",
                "@cp app/B 2 5",
            ) + "app/C id: 3 su",
        )

        val merged = directory.resolve("merged.classlist")
        (first + second).write(merged)

        merged.readText() shouldBe lines(
            "java/lang/Object id: 0",
            "app/A id: 1",
            "@lambda-proxy app/A run ()V",
            "app/B id: 2",
            "@cp app/B 2 5",
        )
    }

    private fun lines(vararg lines: String) = lines.joinToString("\n", postfix = "\n")
}
