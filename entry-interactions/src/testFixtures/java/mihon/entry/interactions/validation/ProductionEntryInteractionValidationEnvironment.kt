package mihon.entry.interactions.validation

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.feature.runtime.registerProfilePreferenceOwners
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProductionEntryInteractionValidationEnvironment(
    private val temporaryDirectory: File,
) : AutoCloseable {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val application: Application = mockk<Application>(relaxed = true).also { application ->
        every { application.cacheDir } returns temporaryDirectory.resolve("cache").also(File::mkdirs)
        every { application.filesDir } returns temporaryDirectory.resolve("files").also(File::mkdirs)
        // Production graph validation runs without Android system services. Keep that boundary
        // explicit so generic service lookups return absence instead of relaxed mock placeholders.
        every { application.getSystemService(any<Class<Any>>()) } returns null
    }

    val appServices = ControlledAppServiceBindings(application, appScope)

    val profilePreferenceOwners = ProfilePreferenceOwnerInstaller(
        owners = ProfilePreferenceOwnerRegistry(),
        preferenceStore = ::InMemoryPreferenceStore,
    )

    init {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    fun composition(graph: ProductionValidationGraph): EntryInteractionComposition {
        graph.registerProfilePreferenceOwners()
        graph.featureRuntimeContributions.validate(graph.featureRuntimeComposition.evaluation)
        return graph.entryInteractionComposition
    }

    override fun close() {
        appScope.cancel()
        Dispatchers.resetMain()
    }
}
