package mihon.entry.interactions.book.epub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.EntryChildWebViewAction
import mihon.entry.interactions.EntryChildWebViewActionsMenu
import mihon.entry.interactions.EntryChildWebViewResolution
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ReadiumReaderTopBar(
    bookTitle: String,
    sectionTitle: String?,
    onClose: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(bookTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                sectionTitle?.let {
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.action_close),
                )
            }
        },
        actions = {
            EntryChildWebViewActionsMenu(
                resolution = childWebView,
                onAction = onChildWebViewAction,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
internal fun ReadiumReaderBottomBar(
    onOpenToc: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
        IconButton(onClick = onOpenToc) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ViewList,
                contentDescription = stringResource(MR.strings.book_table_of_contents),
            )
        }
    }
}
