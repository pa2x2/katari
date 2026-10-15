package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.domain.extension.model.ExtensionStore
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.simpleicons.Discord
import mihon.icons.simpleicons.SimpleIcons
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionStoresContent(
    repos: List<ExtensionStore>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        modifier = modifier,
    ) {
        repos.forEach {
            item {
                ExtensionStoresListItem(
                    modifier = Modifier.animateItem(),
                    store = it,
                    onOpenWebsite = { onOpenWebsite(it) },
                    onOpenDiscord = { onOpenDiscord(it) },
                    onCopy = { onCopy(it) },
                    onDelete = { onClickDelete(it) },
                )
            }
        }
    }
}

@Composable
private fun ExtensionStoresListItem(
    store: ExtensionStore,
    onOpenWebsite: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, style = CardStyle.Elevated) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = KatariTheme.spacing.l, top = KatariTheme.spacing.l, end = KatariTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = MaterialSymbols.AutoMirroredRounded.Label, contentDescription = null)
            Text(text = store.name, role = TextRole.SectionTitle)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                icon = MaterialSymbols.Rounded.OpenInBrowser,
                contentDescription = stringResource(MR.strings.action_open_in_browser),
                onClick = onOpenWebsite,
            )
            if (store.contact.discord != null) {
                IconButton(
                    icon = SimpleIcons.Discord,
                    contentDescription = stringResource(MR.strings.action_open_discord),
                    onClick = onOpenDiscord,
                )
            }
            IconButton(
                icon = MaterialSymbols.Rounded.ContentCopy,
                contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                onClick = onCopy,
            )
            IconButton(
                icon = MaterialSymbols.Rounded.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                onClick = onDelete,
            )
        }
    }
}
