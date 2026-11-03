package mihon.gradle.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MetroContributionVisibilityRulesTest {

    @Test
    fun `a contribution declared less than public outside the graph modules is reported`() {
        val code = """
            @Inject
            @ViewModelKey
            @ContributesIntoMap(AppUiScope::class)
            internal class HiddenViewModel(private val store: Store) : ViewModel()

            internal class Screen(val title: String) {
                @dev.zacsweers.metro.ContributesIntoMap(AppUiScope::class)
                class Model : ViewModel()

                @AssistedFactory
                @ContributesIntoMap(AppUiScope::class)
                private interface Factory
            }

            // @ContributesTo(AppScope::class) internal interface Commented
            @ContributesTo(AppScope::class)
            interface Bindings {
                @Provides
                private fun store(): Store = Store("internal class")
            }
        """.trimIndent()

        val findings = checkMetroContributionVisibility(
            listOf(
                MetroContributionSource("app-ui/src/commonMain/kotlin/Screens.kt", code),
                MetroContributionSource("desktop/src/main/kotlin/Graph.kt", code),
                MetroContributionSource("app-ui/src/commonTest/kotlin/ScreensTest.kt", code),
            ),
        )

        assertEquals(
            listOf("app-ui/src/commonMain/kotlin/Screens.kt:4", "app-ui/src/commonMain/kotlin/Screens.kt:12"),
            findings.map { "${it.relativePath}:${it.lineNumber}" },
        )
    }
}
