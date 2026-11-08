package app.katari.translation.ui.picker.engine

import android.R
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.theme.KatariPreviewTheme
import app.katari.translation.api.engine.KnownTranslationEngine
import app.katari.translation.api.engine.TranslationEngineAction
import app.katari.translation.api.engine.TranslationEngineArtwork
import app.katari.translation.api.engine.TranslationEngineBuildAvailability
import app.katari.translation.api.engine.TranslationEngineDetails
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineState
import app.katari.translation.api.engine.TranslationEngineStatus
import app.katari.translation.api.engine.TranslationProviderId

@Preview(name = "Full picker")
@Preview(name = "Full picker dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TranslationEnginePickerPreview() {
    KatariPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = PREVIEW_READY_ENGINE.id,
                onSelect = {},
                showManagementActions = true,
                showMissingSelectionNotice = true,
            )
        }
    }
}

@Preview(name = "Compact picker", widthDp = 360, heightDp = 640)
@Preview(name = "Large font", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun TranslationEngineSessionPickerPreview() {
    KatariPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = PREVIEW_UNAVAILABLE_ENGINE.id,
                onSelect = {},
                selectableOnly = true,
            )
        }
    }
}

@Preview(name = "Missing stored engine", widthDp = 400, heightDp = 720)
@Composable
private fun TranslationEngineMissingSelectionPreview() {
    KatariPreviewTheme {
        Surface {
            TranslationEnginePickerList(
                engines = previewEngineStates(),
                selected = TranslationEngineId("removed-engine"),
                onSelect = {},
                showManagementActions = true,
                showMissingSelectionNotice = true,
            )
        }
    }
}

private fun previewEngineStates() = listOf(
    TranslationEngineState(
        engine = PREVIEW_READY_ENGINE,
        presentation = null,
        status = TranslationEngineStatus.Ready,
        action = TranslationEngineAction.Setup,
    ),
    TranslationEngineState(
        engine = PREVIEW_UNAVAILABLE_ENGINE,
        presentation = null,
        status = TranslationEngineStatus.NotInstalled,
        action = TranslationEngineAction.Install,
    ),
    TranslationEngineState(
        engine = PREVIEW_CONFIGURE_ENGINE,
        presentation = null,
        status = TranslationEngineStatus.ConfigurationRequired("Enter and test a server endpoint."),
        action = TranslationEngineAction.Configure,
    ),
)

private val PREVIEW_ARTWORK = TranslationEngineArtwork.Bundled(R.drawable.ic_dialog_info)
private val PREVIEW_DETAILS = TranslationEngineDetails(
    description = "Translation through a provider-owned engine.",
    processingLocation = "The configured translation provider.",
    privacyDescription = "The provider privacy policy applies.",
)
private val PREVIEW_READY_ENGINE = previewEngine("android-system", "Android System Translation")
private val PREVIEW_UNAVAILABLE_ENGINE = previewEngine("offline-translator", "Offline Translator")
private val PREVIEW_CONFIGURE_ENGINE = previewEngine("libretranslate-server", "LibreTranslate Server")

private fun previewEngine(id: String, name: String) = KnownTranslationEngine(
    id = TranslationEngineId(id),
    providerId = TranslationProviderId("preview-$id"),
    providerName = "Preview provider",
    engineName = name,
    buildAvailability = TranslationEngineBuildAvailability.Included,
    artwork = PREVIEW_ARTWORK,
    details = PREVIEW_DETAILS,
)
