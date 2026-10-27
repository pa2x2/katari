package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
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
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
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
    RowList(
        state = lazyListState,
        contentPadding = paddingValues,
        modifier = modifier,
    ) {
        itemsIndexed(repos, key = { _, store -> store.indexUrl }) { index, store ->
            RowGroupItem(
                position = RowGroupPosition.of(index, repos.size),
                modifier = Modifier.animateItem(),
            ) {
                ExtensionStoreRow(
                    store = store,
                    onOpenWebsite = { onOpenWebsite(store) },
                    onOpenDiscord = { onOpenDiscord(store) },
                    onCopy = { onCopy(store) },
                    onDelete = { onClickDelete(store) },
                )
            }
        }
    }
}

@Composable
private fun ExtensionStoreRow(
    store: ExtensionStore,
    onOpenWebsite: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    SettingsRow(
        title = store.name,
        subtitle = stringResource(MR.strings.ext_not_signed).takeIf { store.signingKey == null },
        icon = MaterialSymbols.AutoMirroredRounded.Label,
        trailing = {
            Row {
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
        },
    )
}
