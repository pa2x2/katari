package mihon.presentation.kit.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Dimensions that repeat across the app. One-off dimensions belong to the kit component that needs them. */
object KatariSizes {
    val iconSmall: Dp = 18.dp
    val icon: Dp = 24.dp
    val iconLarge: Dp = 32.dp

    /** Engine and provider artwork in cards. */
    val artwork: Dp = 56.dp

    /** Artwork at the top of a details sheet or screen, such as an extension's icon. */
    val artworkLarge: Dp = 72.dp

    /** Smallest touch target an interactive element may have. */
    val touchTarget: Dp = 48.dp

    /** Touch target of controls laid over full-screen media, which are reached for mid-gesture or at arm's length. */
    val touchTargetLarge: Dp = 56.dp

    /** Hairline borders and dividers. */
    val border: Dp = 1.dp

    /** Border of a selected card or chip. */
    val selectedBorder: Dp = 2.dp

    /** Shadow under elevated cards and filled buttons. */
    val shadow: Dp = 1.dp

    /** Widest a column of cards or text grows on tablets and in landscape, so lines stay readable. */
    val readableWidth: Dp = 720.dp

    val none: Dp = 0.dp
}
