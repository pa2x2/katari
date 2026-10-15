package mihon.translation.ui.picker.engine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDropDown
import mihon.icons.materialsymbols.rounded.Translate
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

@Composable
fun TranslationEngineSelectorRow(
    engineName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.l, vertical = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(MaterialSymbols.Rounded.Translate, contentDescription = null, tone = ContentTone.Primary)
        Text(
            text = engineName,
            modifier = Modifier.weight(1f),
            role = TextRole.ItemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(MaterialSymbols.Rounded.ArrowDropDown, contentDescription = null)
    }
}
