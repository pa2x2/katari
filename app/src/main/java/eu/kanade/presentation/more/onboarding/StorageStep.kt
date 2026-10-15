package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class StorageStep(
    storagePreferences: StoragePreferences,
    private val migratingFromMihon: Boolean,
) : OnboardingStep {

    private val storagePref = storagePreferences.baseStorageDirectory

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(
                stringResource(
                    MR.strings.onboarding_storage_info,
                    stringResource(MR.strings.app_name),
                    SettingsDataScreen.storageLocationText(storagePref),
                ),
            )

            if (migratingFromMihon) {
                Text(
                    text = stringResource(MR.strings.onboarding_mihon_migration_storage),
                    role = TextRole.Body,
                )
            }

            Button(
                label = stringResource(MR.strings.onboarding_storage_action_select),
                onClick = {
                    try {
                        pickStorageLocation.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        context.toast(MR.strings.file_picker_error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = KatariTheme.spacing.s))

            Text(stringResource(MR.strings.onboarding_storage_help_info, stringResource(MR.strings.app_name)))
            Button(
                label = stringResource(MR.strings.onboarding_storage_help_action),
                onClick = { handler.openUri(SettingsDataScreen.HELP_URL) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        LaunchedEffect(Unit) {
            storagePref.changes()
                .collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}
