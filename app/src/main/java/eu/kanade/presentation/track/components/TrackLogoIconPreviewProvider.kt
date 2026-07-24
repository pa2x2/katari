package eu.kanade.presentation.track.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.tachiyomi.R

internal data class TrackLogoIconPreviewData(
    val name: String,
    val logoResource: Int,
)

internal class TrackLogoIconPreviewProvider : PreviewParameterProvider<TrackLogoIconPreviewData> {

    override val values: Sequence<TrackLogoIconPreviewData>
        get() = sequenceOf(
            TrackLogoIconPreviewData(
                name = "Dummy Tracker",
                logoResource = R.drawable.brand_anilist,
            ),
        )
}
