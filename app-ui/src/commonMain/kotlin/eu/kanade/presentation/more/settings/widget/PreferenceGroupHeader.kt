package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.surface.pulsingHighlight

@Composable
fun PreferenceGroupHeader(
    title: String,
    isProfileSpecific: Boolean = false,
    highlighted: Boolean = false,
) {
    RowGroupHeader(
        text = title,
        modifier = Modifier.pulsingHighlight(Unit.takeIf { highlighted }),
        trailing = profileSpecificSuffix(isProfileSpecific),
    )
}
