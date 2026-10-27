package eu.kanade.presentation.track.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mihon.trackers.resources.Res
import mihon.trackers.resources.brand_anilist
import org.jetbrains.compose.resources.DrawableResource

internal data class TrackLogoIconPreviewData(
    val name: String,
    val logoResource: DrawableResource,
)

internal class TrackLogoIconPreviewProvider : PreviewParameterProvider<TrackLogoIconPreviewData> {

    override val values: Sequence<TrackLogoIconPreviewData>
        get() = sequenceOf(
            TrackLogoIconPreviewData(
                name = "Dummy Tracker",
                logoResource = Res.drawable.brand_anilist,
            ),
        )
}
