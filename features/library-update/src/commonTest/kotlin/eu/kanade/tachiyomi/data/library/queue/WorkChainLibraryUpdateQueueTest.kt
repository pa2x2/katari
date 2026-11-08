package eu.kanade.tachiyomi.data.library.queue

import eu.kanade.tachiyomi.data.library.LibraryUpdateInput
import eu.kanade.tachiyomi.data.library.LibraryUpdateSelectionFile
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileDatabase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.library.update.model.LibraryUpdateState
import java.io.File

/** The rules every platform's library update queue follows, over a chain that runs one update at a time. */
class WorkChainLibraryUpdateQueueTest {

    @TempDir
    lateinit var files: File

    private val chain = OneAtATimeChain()
    private val active = MutableActiveProfile()
    private val queue by lazy {
        WorkChainLibraryUpdateQueue(
            chain = chain,
            selectionFile = LibraryUpdateSelectionFile(TempDirectories(files)),
            profileDatabase = mockk {
                coEvery { getProfileById(MAIN) } returns profile(MAIN, "Main")
                coEvery { getProfileById(KIDS) } returns profile(KIDS, "Kids")
            },
            activeProfileProvider = active,
        )
    }

    // A second profile's update has to wait for the running one rather than run beside it, and say whose it waits for.
    @Test
    fun `another profile's update waits behind the running one`() = runTest {
        active.activeProfileId = MAIN
        queue.startNow() shouldBe LibraryUpdateStart.Started
        active.activeProfileId = KIDS
        queue.startNow() shouldBe LibraryUpdateStart.Queued("Main")

        queue.stateFlow(KIDS).first() shouldBe LibraryUpdateState.Waiting("Main")
        chain.currentUpdates().map { it.profileId to it.isRunning } shouldBe listOf(MAIN to true, KIDS to false)
    }

    // Starting an update for a profile that has one running or waiting, from any entry point, must not queue another
    // run of the same library, and a selection it turns away must not leave its file behind.
    @Test
    fun `a profile's update is queued only once, running or waiting`() = runTest {
        active.activeProfileId = KIDS
        queue.startNow()
        active.activeProfileId = MAIN
        queue.startNow()

        queue.startNow() shouldBe LibraryUpdateStart.AlreadyQueued
        queue.startSelection(listOf(1L, 2L)) shouldBe LibraryUpdateStart.AlreadyQueued
        queue.startAutomatic(KIDS) shouldBe LibraryUpdateStart.AlreadyQueued

        chain.currentUpdates().map { it.profileId } shouldBe listOf(KIDS, MAIN)
        files.resolve("library_update_selection").listFiles().orEmpty().toList().shouldBeEmpty()
    }

    // Stopping a waiting update has to take it out of the queue, or the profile would count as queued and could never
    // be updated again, while the running update of another profile goes on.
    @Test
    fun `stopping a waiting update clears its profile from the queue`() = runTest {
        active.activeProfileId = MAIN
        queue.startNow()
        active.activeProfileId = KIDS
        queue.startNow()

        queue.stop(KIDS)

        queue.stateFlow(KIDS).first() shouldBe null
        chain.currentUpdates().map { it.profileId to it.isRunning } shouldBe listOf(MAIN to true)
        queue.startNow() shouldBe LibraryUpdateStart.Queued("Main")
    }

    /**
     * Keeps WorkManager's guarantees for a chain: the first update runs, the rest wait in order, and an update taken out
     * ends without the ones behind it failing, the next one then running.
     */
    private class OneAtATimeChain : LibraryUpdateWorkChain {
        private val chained = MutableStateFlow<List<ChainedLibraryUpdate>>(emptyList())
        private var nextId = 0
        private var clock = 0L

        override val updates: Flow<List<ChainedLibraryUpdate>> = chained

        override suspend fun currentUpdates(): List<ChainedLibraryUpdate> = chained.value

        override suspend fun append(input: LibraryUpdateInput) {
            val update = ChainedLibraryUpdate(
                id = "${nextId++}",
                profileId = input.profileId,
                queuedAt = clock++,
                isRunning = false,
                progress = null,
            )
            chained.value = (chained.value + update).runningFirst()
        }

        override suspend fun remove(update: ChainedLibraryUpdate) {
            chained.value = chained.value.filter { it.id != update.id }.runningFirst()
        }

        private fun List<ChainedLibraryUpdate>.runningFirst() = mapIndexed { index, update ->
            update.copy(isRunning = index == 0)
        }
    }

    private class MutableActiveProfile : ActiveProfileProvider {
        override var activeProfileId: Long = MAIN
        override val activeProfileIdFlow: Flow<Long>
            get() = MutableStateFlow(activeProfileId)
    }

    private class TempDirectories(root: File) : AppPrivateDirectories {
        override val cache: File = root
        override val files: File = root
        override val noBackupFiles: File = root
    }

    private companion object {
        const val MAIN = 1L
        const val KIDS = 2L

        fun profile(id: Long, name: String) = Profile(
            id = id,
            uuid = "uuid-$id",
            name = name,
            colorSeed = 0,
            position = id,
            requiresAuth = false,
            isArchived = false,
        )
    }
}
