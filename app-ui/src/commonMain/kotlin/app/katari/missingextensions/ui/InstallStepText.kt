package app.katari.missingextensions.ui

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.extension.model.InstallStep
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What an install row says while its extension installs, or once it has; null while neither, for its details. */
@Composable
internal fun installStepText(installed: Boolean, step: InstallStep?): String? = when {
    installed -> stringResource(MR.strings.ext_installed)
    step == InstallStep.Pending -> stringResource(MR.strings.ext_pending)
    step == InstallStep.Downloading -> stringResource(MR.strings.ext_downloading)
    step == InstallStep.Installing -> stringResource(MR.strings.ext_installing)
    step == InstallStep.Converting -> stringResource(MR.strings.ext_converting)
    step == InstallStep.RequiresUserAction -> stringResource(MR.strings.ext_requires_user_action)
    else -> null
}
