package mihon.translation.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDropDown
import mihon.icons.materialsymbols.rounded.Translate
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.list.SettingsRow

@Composable
fun TranslationEngineSelectorRow(
    engineName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        title = engineName,
        modifier = modifier,
        icon = MaterialSymbols.Rounded.Translate,
        trailing = { Icon(MaterialSymbols.Rounded.ArrowDropDown, contentDescription = null) },
        subtitleMaxLines = 1,
        onClick = onClick,
    )
}
