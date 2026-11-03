package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.app.ui.resources.Res
import mihon.app.ui.resources.ic_katari
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.theme.KatariTheme
import org.jetbrains.compose.resources.painterResource

/** The app logo heading a screen, with [below] under it. */
@Composable
fun LogoHeader(
    iconPadding: PaddingValues = PaddingValues(),
    below: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_katari),
            contentDescription = null,
            modifier = Modifier
                .padding(iconPadding)
                .size(KatariTheme.sizes.artworkLarge),
        )
        below()
    }
}
