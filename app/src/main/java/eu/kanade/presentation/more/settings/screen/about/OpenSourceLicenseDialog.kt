package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.mikepenz.aboutlibraries.entity.Library
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A library's licence texts. A library whose generated data carries no licence text shows the licence names instead,
 * and its website stays one tap away.
 */
@Composable
internal fun OpenSourceLicenseDialog(library: Library, onDismissRequest: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val website = library.website ?: library.scm?.url
    val licenses = library.licenses.map { it.licenseContent?.takeIf(String::isNotBlank) ?: it.name }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = library.name,
        confirm = DialogAction(stringResource(MR.strings.action_close), onDismissRequest),
        neutral = website?.let { DialogAction(stringResource(MR.strings.website), { uriHandler.openUri(it) }) },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        ) {
            licenses.forEach { Text(it, role = TextRole.Supporting) }
        }
    }
}
