package eu.kanade.presentation.category.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.icons.materialsymbols.rounded.DragHandle
import app.katari.icons.materialsymbols.rounded.Edit
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReorderableCollectionItemScope.CategoryListItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        title = category.name,
        modifier = modifier,
        leading = {
            Icon(
                imageVector = MaterialSymbols.Rounded.DragHandle,
                contentDescription = null,
                modifier = Modifier
                    .draggableHandle()
                    .padding(KatariTheme.spacing.s),
                tone = ContentTone.Secondary,
            )
        },
        trailing = {
            Row {
                IconButton(
                    icon = MaterialSymbols.Rounded.Edit,
                    contentDescription = stringResource(MR.strings.action_edit_category),
                    onClick = onEdit,
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                    onClick = onDelete,
                )
            }
        },
        onClick = onEdit,
    )
}
