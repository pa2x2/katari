package mihon.gradle.tasks

import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class EntryMigrationBoundaryRulesTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `application consumers cannot access raw Migration provider contracts`() {
        createFixture(
            appSource = """
                package app

                class AppFeature(
                    private val provider: EntryMigrationProvider,
                ) {
                    val binding = EntryMigrationCapability
                }
            """.trimIndent(),
            additionalFiles = mapOf(
                "entry-interactions/spi/src/main/java/test/EntryMigrationProvider.kt" to
                    """
                        package test

                        interface EntryMigrationProvider

                        val EntryMigrationCapability = Any()
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "EntryMigrationProvider is root/type-module Entry interaction internals"
        error.message shouldContain "EntryMigrationCapability is root/type-module Entry interaction internals"
    }

    @Test
    fun `application consumers cannot recreate legacy Migration authorities`() {
        createFixture(
            appSource = """
                package app

                class AppFeature(
                    private val capability: EntryCapabilityInteraction,
                    private val migrate: MigrateEntryUseCase,
                ) {
                    fun supportsMigration(entry: Entry) = true
                }
            """.trimIndent(),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "EntryCapabilityInteraction must be consumed through EntryMigrationFeature"
        error.message shouldContain "MigrateEntryUseCase must be consumed through EntryMigrationFeature"
        error.message shouldContain "supportsMigration must be consumed through EntryMigrationFeature"
    }

    @Test
    fun `Migration implementation cannot read ambient options or authorize a concrete type`() {
        createFixture(
            additionalFiles = mapOf(
                "entry-interactions/src/main/java/mihon/entry/interactions/migration/EntryMigrationCoordinator.kt" to
                    """
                        package mihon.entry.interactions

                        class EntryMigrationCoordinator(
                            private val preferences: SourcePreferences,
                        ) {
                            val flags = preferences.migrationFlags.get()
                            val supported = EntryType.AUDIO
                        }
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "captured intent, not ambient authority: SourcePreferences"
        error.message shouldContain "captured intent, not ambient authority: migrationFlags"
        error.message shouldContain "cannot authorize behavior with a concrete EntryType: AUDIO"
    }

    @Test
    fun `unrelated feature and application api cannot borrow Migration host ports`() {
        createFixture(
            additionalFiles = hostApiFixture() + mapOf(
                "entry-interactions/src/main/java/mihon/entry/interactions/download/EntryDownloadFeature.kt" to
                    """
                        package mihon.entry.interactions

                        class EntryDownloadFeature(private val host: EntryMigrationPreparationHost)
                    """.trimIndent(),
                "entry-interactions/api/src/main/java/mihon/entry/interactions/migration/EntryMigrationFeature.kt" to
                    """
                        package mihon.entry.interactions

                        interface EntryMigrationFeature {
                            val host: EntryMigrationPreparationHost
                        }
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain
            "EntryMigrationPreparationHost is an application host port reserved for the root Migration coordinator"
    }

    @Test
    fun `root Migration coordinator and segregated app adapter may access host ports`() {
        createFixture(
            additionalFiles = hostApiFixture() + mapOf(
                "entry-interactions/src/main/java/mihon/entry/interactions/migration/EntryMigrationCoordinator.kt" to
                    """
                        package mihon.entry.interactions

                        class EntryMigrationCoordinator(private val host: EntryMigrationPreparationHost)
                    """.trimIndent(),
                "app/src/main/java/mihon/entry/interactions/host/AppEntryMigrationPreparationHost.kt" to
                    """
                        package mihon.entry.interactions.host

                        class AppEntryMigrationPreparationHost : EntryMigrationPreparationHost
                    """.trimIndent(),
            ),
        )

        runBoundaryCheck()
    }

    @Test
    fun `type module may own Migration provider participation`() {
        createFixture(
            additionalFiles = mapOf(
                "entry-interactions/anime/src/main/java/test/AnimeMigrationProvider.kt" to
                    """
                        package test

                        internal class AnimeMigrationProvider : EntryMigrationProvider {
                            val binding = EntryMigrationCapability.bind(this)
                        }
                    """.trimIndent(),
            ),
        )

        runBoundaryCheck()
    }

    private fun hostApiFixture(): Map<String, String> = mapOf(
        MIGRATION_PREPARATION_HOST_PATH to
            """
                package mihon.entry.interactions.host

                interface EntryMigrationPreparationHost
            """.trimIndent(),
    )

    private fun createFixture(
        appSource: String = """
            package app

            class AppFeature
        """.trimIndent(),
        additionalFiles: Map<String, String> = emptyMap(),
    ) {
        write(
            "app/build.gradle.kts",
            """
                dependencies {
                    implementation(projects.entryInteractions)
                }
            """.trimIndent(),
        )
        write("app/src/main/java/app/AppFeature.kt", appSource)
        additionalFiles.forEach { (path, content) -> write(path, content) }
    }

    private fun runBoundaryCheck() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register(
            "checkEntryInteractionBoundaries",
            EntryInteractionBoundaryCheckTask::class.java,
        ) {
            repositoryRoot.set(tempDir.toFile())
        }.get()

        task.action()
    }

    private fun write(relativePath: String, content: String) {
        val file = tempDir.resolve(relativePath)
        file.parent.createDirectories()
        file.writeText(content.trimIndent() + "\n")
    }

    private companion object {
        const val MIGRATION_PREPARATION_HOST_PATH =
            "entry-interactions/api/src/main/java/mihon/entry/interactions/migration/host/" +
                "EntryMigrationPreparationHost.kt"
    }
}
