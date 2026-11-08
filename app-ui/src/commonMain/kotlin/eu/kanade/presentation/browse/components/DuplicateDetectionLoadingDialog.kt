package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.dialog.ProgressDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DuplicateDetectionLoadingDialog() {
    ProgressDialog(
        title = stringResource(MR.strings.pref_duplicate_detection),
        message = stringResource(MR.strings.duplicate_detection_checking),
    )
}
