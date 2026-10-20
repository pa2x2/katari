package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme

/** The app logo heading a screen, with [below] between the logo and the divider that ends the header. */
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
            painter = painterResource(R.drawable.ic_katari),
            contentDescription = null,
            modifier = Modifier
                .padding(iconPadding)
                .size(KatariTheme.sizes.artworkLarge),
        )
        below()

        HorizontalDivider()
    }
}
