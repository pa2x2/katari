package mihon.entry.viewer.settings.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * What the open reader shows with a shared setting for the entry it reads.
 *
 * Shared declarations are profile-wide and also appear outside readers, so anything that depends on the entry, such
 * as its own values or actions that open reader surfaces, is contributed here by the reader instead.
 *
 * @property summary replaces the declaration's summary while the setting is available.
 * @property content rows shown right below the setting.
 */
class ReaderSharedSettingDetails(
    val summary: String? = null,
    val content: @Composable ColumnScope.() -> Unit = {},
)
