package app.katari.desktop.application.classdata

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

class ClassDataFolderTest {

    @TempDir
    lateinit var directory: Path

    private val build by lazy { directory.resolve("build") }
    private val folder by lazy { ClassDataFolder(build) }

    // The JVM goes on writing the record of a run still going, whose classes after the collection would be lost with
    // it, while the record of every run that ended would otherwise pile up in the cache
    @Test
    fun `records of ended runs are collected once and deleted, a running one is kept`() {
        record(processId = 10, "app/Ended")
        record(processId = 20, "app/Running")

        folder.collectSessions { it == 20L }.classCount shouldBe 2
        sessions().map { it.name } shouldBe listOf("pid20.classlist")

        record(processId = 20, "app/Running", "app/Later")
        folder.collectSessions { false }.classCount shouldBe 3
        sessions() shouldBe emptyList()
    }

    // Counting what runs added against the whole collection instead of what the archive was built from would never
    // build it again, leaving out every screen the first runs didn't open
    @Test
    fun `the archive is built again once runs have added enough classes it lacks`() {
        record(processId = 1, *classes(0 until 10))
        val first = folder.collectSessions { false }
        folder.needsBuild(first) shouldBe true

        folder.building.writeText("archive")
        folder.keepBuilt(first)
        folder.needsBuild(first) shouldBe false

        record(processId = 2, *classes(0 until 310))
        folder.needsBuild(folder.collectSessions { false }) shouldBe true
    }

    private fun record(processId: Long, vararg classes: String) {
        val sessions = build.resolve("sessions").createDirectories()
        sessions.resolve("pid$processId.classlist")
            .writeText(classes.withIndex().joinToString("") { (id, name) -> "$name id: $id\n" })
    }

    private fun sessions() = build.resolve("sessions").listDirectoryEntries()

    private fun classes(range: IntRange) = range.map { "app/C$it" }.toTypedArray()
}
