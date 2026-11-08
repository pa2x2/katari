package app.katari.sync.ui.question

import androidx.compose.runtime.Composable
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The device as a caption names it, such as "This device" before when it wrote the notes (D005). */
@Composable
internal fun MadeOn.label(): String = when (this) {
    MadeOn.ThisDevice -> stringResource(MR.strings.sync_this_device)
    is MadeOn.Other -> name ?: stringResource(MR.strings.sync_unknown_device)
}

/** The device as a sentence names it, such as "Deleted on this device". */
@Composable
internal fun MadeOn.inText(): String = when (this) {
    MadeOn.ThisDevice -> stringResource(MR.strings.sync_this_device_in_text)
    is MadeOn.Other -> name ?: stringResource(MR.strings.sync_unknown_device)
}
