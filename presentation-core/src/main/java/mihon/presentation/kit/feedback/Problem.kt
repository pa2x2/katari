package mihon.presentation.kit.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Lock
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A way out of a problem. The first action of a problem is its main one. */
data class ProblemAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
)

/**
 * A full screen saying that what the user opened can't be shown, such as a chapter its source refused. The bar names
 * what was being opened ([title] and [subtitle]) and leads back; the centre says what failed in [heading], why in
 * [message], and whose words [message] are in [attribution] when they come from elsewhere, such as a source.
 */
@Composable
fun ProblemScreen(
    icon: ImageVector,
    heading: String,
    message: String,
    navigateUp: () -> Unit,
    actions: List<ProblemAction>,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    attribution: String? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppBar(title = title, subtitle = subtitle, navigateUp = navigateUp, large = false) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = KatariTheme.sizes.readableWidth)
                    .padding(horizontal = KatariTheme.spacing.xxl, vertical = KatariTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProblemExplanation(
                    icon = icon,
                    heading = heading,
                    message = message,
                    attribution = attribution,
                    headingRole = TextRole.DetailTitle,
                )
                // Stacked actions keep a button's width on wide screens instead of stretching across them.
                Column(
                    modifier = Modifier
                        .padding(top = KatariTheme.spacing.xxl)
                        .widthIn(max = KatariTheme.sizes.readableWidth / 2),
                    verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                ) {
                    actions.forEachIndexed { index, action ->
                        ProblemActionButton(action, main = index == 0, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/**
 * A problem inside content that keeps showing, such as the next chapter failing to open under the one being read. It
 * says the same as [ProblemScreen] in a card that takes its colours from the surrounding theme.
 */
@Composable
fun ProblemCard(
    icon: ImageVector,
    heading: String,
    message: String,
    actions: List<ProblemAction>,
    modifier: Modifier = Modifier,
    attribution: String? = null,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProblemExplanation(
                icon = icon,
                heading = heading,
                message = message,
                attribution = attribution,
                headingRole = TextRole.SheetTitle,
            )
            if (actions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = KatariTheme.spacing.xl),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                ) {
                    actions.forEachIndexed { index, action ->
                        ProblemActionButton(action, main = index == 0, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemExplanation(
    icon: ImageVector,
    heading: String,
    message: String,
    attribution: String?,
    headingRole: TextRole,
) {
    Surface(container = SurfaceContainer.SecondaryContainer, shape = KatariTheme.shapes.full) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(KatariTheme.spacing.l)
                .size(KatariTheme.sizes.iconLarge),
        )
    }
    Text(
        text = heading,
        modifier = Modifier.padding(top = KatariTheme.spacing.xl),
        role = headingRole,
        textAlign = TextAlign.Center,
    )
    Text(
        text = message,
        modifier = Modifier.padding(top = KatariTheme.spacing.m),
        role = TextRole.Body,
        tone = ContentTone.Secondary,
        textAlign = TextAlign.Center,
    )
    attribution?.let {
        Text(
            text = it,
            modifier = Modifier.padding(top = KatariTheme.spacing.m),
            role = TextRole.Caption,
            tone = ContentTone.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProblemActionButton(action: ProblemAction, main: Boolean, modifier: Modifier) {
    Button(
        label = action.label,
        onClick = action.onClick,
        modifier = modifier,
        style = if (main) ButtonStyle.Filled else ButtonStyle.Tonal,
        icon = action.icon,
    )
}

@PreviewLightDark
@Composable
private fun ProblemScreenPreview() {
    ProblemScreen(
        icon = MaterialSymbols.Rounded.Lock,
        heading = "Couldn’t open this chapter",
        message = "This chapter is locked. Unlock it on the website to read it.",
        navigateUp = {},
        actions = listOf(
            ProblemAction("WebView", {}, MaterialSymbols.Rounded.Public),
            ProblemAction("Retry", {}, MaterialSymbols.Rounded.Refresh),
        ),
        title = "Reborn as the Beast King",
        subtitle = "Chapter 41: The Northern Gate",
        attribution = "Message from Beast Novels",
    )
}
