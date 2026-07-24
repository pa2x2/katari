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

class EntryMergeBoundaryRulesTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `application-facing Merge api cannot expose raw membership service`() {
        createFixture(
            additionalFiles = mapOf(
                "entry-interactions/api/src/main/java/mihon/entry/interactions/merge/EntryMergeFeature.kt" to
                    """
                        package mihon.entry.interactions

                        interface EntryMergeFeature {
                            suspend fun members(entryId: Long): List<EntryMergeMember>
                            suspend fun deleteGroup(targetId: Long)
                        }

                        data class EntryMergeGroup(val targetId: Long)
                        data class EntryMergeMember(val entryId: Long)
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "application-facing Merge API cannot expose raw membership type: EntryMergeGroup"
        error.message shouldContain "application-facing Merge API cannot expose raw membership type: EntryMergeMember"
        error.message shouldContain "application-facing Merge API cannot expose raw membership operation: members"
        error.message shouldContain "application-facing Merge API cannot expose raw membership operation: deleteGroup"
    }

    @Test
    fun `application-facing Merge api cannot expose an existing raw authority`() {
        createFixture(
            additionalFiles = mapOf(
                "entry-interactions/api/src/main/java/mihon/entry/interactions/merge/EntryMergeFeature.kt" to
                    """
                        package mihon.entry.interactions

                        import tachiyomi.domain.entry.model.EntryMerge

                        interface EntryMergeFeature {
                            suspend fun rawState(): List<EntryMerge>
                        }
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "raw Merge authority cannot cross the Merge application or host API boundary"
        error.message shouldContain "EntryMerge"
    }

    @Test
    fun `application consumers cannot access Merge host ports`() {
        createFixture(
            appSource = """
                package app

                import mihon.entry.interactions.host.EntryMergeHost

                class AppFeature(private val host: EntryMergeHost)
            """.trimIndent(),
            additionalFiles = hostApiFixture(),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain
            "EntryMergeHost is an application host port reserved for the root Merge coordinator"
    }

    @Test
    fun `segregated adapter and root coordinator may access Entry host ports`() {
        createFixture(
            additionalFiles = hostApiFixture() + mapOf(
                "app/src/main/java/mihon/entry/interactions/host/AppEntryMergeHost.kt" to
                    """
                        package mihon.entry.interactions.host

                        class AppEntryMergeHost : EntryMergeHost
                    """.trimIndent(),
                "entry-interactions/src/main/java/mihon/entry/interactions/merge/EntryMergeCoordinator.kt" to
                    """
                        package mihon.entry.interactions

                        import mihon.entry.interactions.host.EntryMergeHost

                        internal class EntryMergeCoordinator(private val host: EntryMergeHost)
                    """.trimIndent(),
            ),
        )

        runBoundaryCheck()
    }

    @Test
    fun `unrelated root feature cannot borrow Merge host ports`() {
        createFixture(
            additionalFiles = hostApiFixture() + mapOf(
                "entry-interactions/src/main/java/mihon/entry/interactions/download/EntryDownloadFeature.kt" to
                    """
                        package mihon.entry.interactions

                        import mihon.entry.interactions.host.EntryMergeHost

                        internal class EntryDownloadFeature(private val host: EntryMergeHost)
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain
            "EntryMergeHost is an application host port reserved for the root Merge coordinator"
    }

    @Test
    fun `Merge implementation cannot use ambient profile authority or concrete type gates`() {
        createFixture(
            additionalFiles = mapOf(
                "entry-interactions/src/main/java/mihon/entry/interactions/merge/EntryMergeCoordinator.kt" to
                    """
                        package mihon.entry.interactions

                        class EntryMergeCoordinator(
                            private val profiles: ActiveProfileProvider,
                        ) {
                            val supported = EntryType.AUDIO
                            val profileId = profiles.activeProfileId
                        }
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "not ambient profile authority: ActiveProfileProvider"
        error.message shouldContain "not ambient profile authority: activeProfileId"
        error.message shouldContain "cannot gate behavior on a concrete current EntryType: AUDIO"
    }

    @Test
    fun `placing a consumer in the host package does not grant host access`() {
        createFixture(
            additionalFiles = hostApiFixture() + mapOf(
                "app/src/main/java/mihon/entry/interactions/host/EntryMergeScreen.kt" to
                    """
                        package mihon.entry.interactions.host

                        class EntryMergeScreen(private val host: EntryMergeHost)
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain
            "EntryMergeHost is an application host port reserved for the root Merge coordinator"
    }

    @Test
    fun `application consumers cannot use raw Merge authorities`() {
        createFixture(
            appSource = """
                package app

                class AppFeature(
                    private val getMergedEntry: GetMergedEntry,
                    private val repository: MergedEntryRepository,
                ) {
                    private val rawModel = EntryMerge::class.simpleName
                    private val rawQueries = database.merged_entriesQueries
                }
            """.trimIndent(),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "raw Merge authority must be consumed through Merge intents"
        error.message shouldContain "GetMergedEntry"
        error.message shouldContain "MergedEntryRepository"
        error.message shouldContain "EntryMerge"
        error.message shouldContain "merged_entriesQueries"
    }

    @Test
    fun `only Profile Move coordinator can access Merge transaction participation`() {
        createFixture(
            appSource = """
                package app

                import mihon.entry.interactions.EntryMergeProfileMoveFeature

                class AppFeature(private val merge: EntryMergeProfileMoveFeature)
            """.trimIndent(),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "EntryMergeProfileMoveFeature is reserved for the Profile Move coordinator"
    }

    @Test
    fun `domain consumers cannot retain raw Merge authorities`() {
        createFixture(
            additionalFiles = mapOf(
                "domain/src/main/java/tachiyomi/domain/entry/GetLibraryEntries.kt" to
                    """
                        package tachiyomi.domain.entry

                        import tachiyomi.domain.entry.repository.MergedEntryRepository

                        class GetLibraryEntries(private val merges: MergedEntryRepository)
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "raw Merge authority must be consumed through Merge intents"
        error.message shouldContain "MergedEntryRepository"
    }

    @Test
    fun `type modules and consumers cannot restore a Merge support gate`() {
        createFixture(
            appSource = """
                package app

                class AppFeature {
                    fun supportsMerge(): Boolean = true
                }
            """.trimIndent(),
            additionalFiles = mapOf(
                "entry-interactions/manga/src/main/java/test/MangaPlugin.kt" to
                    """
                        package test

                        class MangaPlugin(private val capability: EntryMergeCapability)
                    """.trimIndent(),
            ),
        )

        val error = assertThrows(GradleException::class.java) { runBoundaryCheck() }

        error.message shouldContain "cannot be gated by transitional type support: supportsMerge"
        error.message shouldContain "cannot be gated by transitional type support: EntryMergeCapability"
    }

    private fun hostApiFixture(): Map<String, String> = mapOf(
        "entry-interactions/api/src/main/java/mihon/entry/interactions/merge/host/EntryMergeHost.kt" to
            """
                package mihon.entry.interactions.host

                interface EntryMergeHost
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
}
