package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Description
import app.katari.presentation.kit.content.breakableFileName
import app.katari.presentation.kit.list.SettingsRow
import tachiyomi.presentation.core.i18n.fileSize

/**
 * The backup file a screen was given, as Bring over and Restore backup show it: its name, or [fallback] when the
 * system doesn't tell it, and its size. [onClick] picks another file.
 */
@Composable
internal fun BackupFileRow(name: String?, size: Long?, fallback: String, onClick: (() -> Unit)? = null) {
    SettingsRow(
        title = name?.let(::breakableFileName) ?: fallback,
        subtitle = size?.let { fileSize(it) },
        icon = MaterialSymbols.Rounded.Description,
        onClick = onClick,
    )
}
