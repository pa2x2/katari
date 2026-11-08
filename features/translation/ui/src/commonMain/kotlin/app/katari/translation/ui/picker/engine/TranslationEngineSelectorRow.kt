package app.katari.translation.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ArrowDropDown
import app.katari.icons.materialsymbols.rounded.Translate
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.list.SettingsRow

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
