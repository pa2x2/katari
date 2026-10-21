package mihon.presentation.kit.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Newspaper
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme

/**
 * A full screen that explains something and asks to accept it, such as onboarding or a crash report. [content] runs the
 * full width below the heading, so it lays out its parts in row group cards like any other page.
 */
@Composable
fun InfoScreen(
    icon: ImageVector,
    headingText: String,
    subtitleText: String,
    acceptText: String,
    onAcceptClick: () -> Unit,
    canAccept: Boolean = true,
    rejectText: String? = null,
    onRejectClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        bottomBar = {
            BottomActionBar {
                if (rejectText != null && onRejectClick != null) {
                    Button(
                        label = rejectText,
                        onClick = onRejectClick,
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Outlined,
                    )
                }
                Button(
                    label = acceptText,
                    onClick = onAcceptClick,
                    modifier = Modifier.weight(1f),
                    enabled = canAccept,
                )
            }
        },
    ) { paddingValues ->
        // Status bar scrim
        Box(
            modifier = Modifier
                .zIndex(2f)
                .background(KatariTheme.colors.background.copy(alpha = STATUS_BAR_SCRIM_ALPHA))
                .fillMaxWidth()
                .height(paddingValues.calculateTopPadding()),
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(top = 48.dp, bottom = KatariTheme.spacing.l),
        ) {
            Column(modifier = Modifier.padding(horizontal = KatariTheme.spacing.l)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = KatariTheme.spacing.s)
                        .size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = headingText,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = subtitleText,
                    modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
                    color = KatariTheme.colors.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Column(modifier = Modifier.padding(top = KatariTheme.spacing.s), content = content)
        }
    }
}

@PreviewLightDark
@Composable
private fun InfoScaffoldPreview() {
    InfoScreen(
        icon = MaterialSymbols.Rounded.Newspaper,
        headingText = "Heading",
        subtitleText = "Subtitle",
        acceptText = "Accept",
        onAcceptClick = {},
        rejectText = "Reject",
        onRejectClick = {},
    ) {
        Text("Hello world")
    }
}

// Content scrolled under the status bar stays faintly visible through the scrim.
private const val STATUS_BAR_SCRIM_ALPHA = 0.78f
