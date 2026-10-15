package mihon.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import dev.icerock.moko.resources.StringResource
import mihon.presentation.kit.button.StackedActionButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.random.Random

data class EmptyScreenAction(
    val stringRes: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun EmptyScreen(
    stringRes: StringResource,
    modifier: Modifier = Modifier,
    actions: List<EmptyScreenAction>? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    EmptyScreen(
        message = stringResource(stringRes),
        modifier = modifier,
        actions = actions,
        content = content,
    )
}

/** Fills a screen whose whole content is empty, with a kaomoji, a [message] and optional [actions]. */
@Composable
fun EmptyScreen(
    message: String,
    modifier: Modifier = Modifier,
    actions: List<EmptyScreenAction>? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val face = remember { getRandomErrorFace() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KatariTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Face(face, TextRole.Display)
        Text(
            text = message,
            modifier = Modifier.paddingFromBaseline(top = KatariTheme.spacing.xl),
            role = TextRole.Body,
            tone = ContentTone.Secondary,
            textAlign = TextAlign.Center,
        )

        if (!actions.isNullOrEmpty()) {
            Row(
                modifier = Modifier.padding(top = KatariTheme.spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                actions.fastForEach {
                    StackedActionButton(
                        modifier = Modifier.weight(1f),
                        label = stringResource(it.stringRes),
                        icon = it.icon,
                        onClick = it.onClick,
                    )
                }
            }
        }

        content()
    }
}

/** Says that a list, picker or sheet section has nothing to show, such as a search with no matches. */
@Composable
fun NoResults(
    modifier: Modifier = Modifier,
    message: String = stringResource(MR.strings.no_results_found),
) {
    val face = remember { getRandomErrorFace() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        Face(face, TextRole.SheetTitle)
        Text(message, role = TextRole.Body, tone = ContentTone.Secondary, textAlign = TextAlign.Center)
    }
}

// Kaomoji are drawn left to right in every locale.
@Composable
private fun Face(face: String, role: TextRole) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(face, role = role, tone = ContentTone.Secondary)
    }
}

private val ErrorFaces = listOf(
    "(･o･;)",
    "Σ(ಠ_ಠ)",
    "ಥ_ಥ",
    "(˘･_･˘)",
    "(；￣Д￣)",
    "(･Д･。",
    "(╬ಠ益ಠ)",
    "(╥﹏╥)",
    "(⋟﹏⋞)",
    "Ò︵Ó",
    " ˙ᯅ˙)",
    "(¬_¬)",
)

private fun getRandomErrorFace(): String {
    return ErrorFaces[Random.nextInt(ErrorFaces.size)]
}
