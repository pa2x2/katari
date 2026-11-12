package app.katari.presentation.kit.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme

/**
 * One of the things a [PickerPopover] offers, such as one of the account's devices. [subtitle] says what sets it apart,
 * such as when the device was last seen, or, for one that isn't [enabled], why it can't be picked.
 */
@Immutable
class PickerChoice(
    val key: Any,
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val subtitleTone: ContentTone = ContentTone.Inherit,
    val enabled: Boolean = true,
)

/**
 * A short list to pick one thing from, opened from a button: a [Popover] beside [anchor]'s button (a sheet in compact
 * windows), headed by [title] and a [subtitle] naming what the pick is for, with a row for each of [choices]. Picking
 * is the action, such as choosing the device a chapter downloads on; [onBack] heads a later step of the same pick,
 * such as how much to download once the device is chosen. The caller closes it, as a pick may open the next step.
 */
@Composable
fun PickerPopover(
    anchor: PopoverAnchor?,
    title: String,
    choices: List<PickerChoice>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    Popover(anchor = anchor, onDismissRequest = onDismissRequest, modifier = modifier) {
        Column {
            SheetHeader(title = title, subtitle = subtitle, onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = KatariTheme.spacing.s),
            ) {
                RowGroup {
                    choices.forEach { choice ->
                        row(key = choice.key) {
                            SettingsRow(
                                title = choice.title,
                                subtitle = choice.subtitle,
                                icon = choice.icon,
                                subtitleTone = choice.subtitleTone,
                                enabled = choice.enabled,
                                onClick = choice.onClick,
                            )
                        }
                    }
                }
            }
        }
    }
}
