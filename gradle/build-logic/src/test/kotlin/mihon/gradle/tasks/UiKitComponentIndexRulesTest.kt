package mihon.gradle.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UiKitComponentIndexRulesTest {

    @Test
    fun `kit composables missing from the index and index names that are not kit composables are reported`() {
        val kit = UiKitBoundarySource(
            "presentation-core/src/commonMain/kotlin/mihon/presentation/kit/surface/Kit.kt",
            """
            /** A card. */
            @Composable
            fun Card(content: @Composable () -> Unit) {}

            @OptIn(
                ExperimentalFoundationApi::class,
            )
            @Composable
            fun Modifier.selectionFill(selected: Boolean): Modifier = this

            @Composable
            expect fun rememberPicker(): Picker

            @Composable
            fun Banner(text: String) {}

            @Composable
            internal fun KitOnly() {}

            fun notComposable() {}

            interface Scope {
                @Composable
                fun Member()
            }
            """.trimIndent(),
        )
        val index = UiKitBoundarySource(
            UI_KIT_COMPONENT_INDEX,
            """
            | Job | Component | Notes |
            |---|---|---|
            | A card | `Card`, `Modifier.selectionFill` | Not a `Surface` |
            | A picker | `rememberPicker` | |
            | A tile | `RemovedTile` | |
            """.trimIndent(),
        )

        val findings = checkUiKitComponentIndex(listOf(kit), index)

        assertEquals(
            listOf(kit.relativePath to 15, UI_KIT_COMPONENT_INDEX to 5),
            findings.map { it.relativePath to it.lineNumber },
        )
    }
}
