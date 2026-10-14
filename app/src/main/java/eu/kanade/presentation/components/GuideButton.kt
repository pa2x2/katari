package eu.kanade.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Opens the website guide's [page], a path such as `profiles/` or `translation/#text-recognition`. */
@Composable
fun GuideButton(
    page: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    IconButton(onClick = { uriHandler.openUri(Constants.URL_GUIDE + page) }, modifier = modifier) {
        Icon(
            imageVector = MaterialSymbols.AutoMirroredRounded.Help,
            contentDescription = stringResource(MR.strings.label_help),
        )
    }
}
