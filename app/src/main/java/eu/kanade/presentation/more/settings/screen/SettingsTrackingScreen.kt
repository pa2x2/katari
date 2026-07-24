package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import mihon.entry.interactions.EntryTrackingAccount
import mihon.entry.interactions.EntryTrackingAccountOperationResult
import mihon.entry.interactions.EntryTrackingCredentialIdentity
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingLoginMethod
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsTrackingScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_tracking

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(onClick = { uriHandler.openUri("https://mihon.app/docs/guides/tracking") }) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = stringResource(MR.strings.tracking_guide),
            )
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val trackPreferences = remember { Injekt.get<TrackPreferences>() }
        val trackingFeature = remember { Injekt.get<EntryTrackingFeature>() }
        val scope = rememberCoroutineScope()
        val accountSnapshot by trackingFeature.observeAccounts().collectAsState(trackingFeature.currentAccounts())

        var dialog by remember { mutableStateOf<Any?>(null) }
        dialog?.run {
            when (this) {
                is LoginDialog -> {
                    TrackingLoginDialog(
                        account = account,
                        storedUsername = storedUsername,
                        storedPassword = storedPassword,
                        trackingFeature = trackingFeature,
                        onDismissRequest = { dialog = null },
                    )
                }
                is LogoutDialog -> {
                    TrackingLogoutDialog(
                        account = account,
                        trackingFeature = trackingFeature,
                        onDismissRequest = { dialog = null },
                    )
                }
            }
        }

        val accounts = accountSnapshot.accounts
        val regularAccounts = accounts.filter { it.loginMethod !is EntryTrackingLoginMethod.Passive }
        val enhancedAccounts = accounts.filter { it.loginMethod is EntryTrackingLoginMethod.Passive }
        val availableEnhancedAccounts = enhancedAccounts.filter(EntryTrackingAccount::isAvailable)
        var enhancedTrackerInfo = stringResource(MR.strings.enhanced_tracking_info)
        val unavailableEnhancedAccounts = enhancedAccounts.filterNot(EntryTrackingAccount::isAvailable)
        if (unavailableEnhancedAccounts.isNotEmpty()) {
            val missingSourcesInfo = stringResource(
                MR.strings.enhanced_services_not_installed,
                unavailableEnhancedAccounts.joinToString { it.service.name },
            )
            enhancedTrackerInfo += "\n\n$missingSourcesInfo"
        }

        fun beginLogin(account: EntryTrackingAccount) {
            when (account.loginMethod) {
                is EntryTrackingLoginMethod.Credentials -> {
                    val credentials = trackingFeature.storedCredentials(account.service.id) ?: return
                    dialog = LoginDialog(account, credentials.username, credentials.password)
                }
                EntryTrackingLoginMethod.External,
                EntryTrackingLoginMethod.Passive,
                -> scope.launchIO {
                    when (val result = trackingFeature.beginLogin(account.service.id)) {
                        is EntryTrackingAccountOperationResult.ExternalAuthorization -> {
                            withUIContext { context.openInBrowser(result.uri, forceDefaultBrowser = true) }
                        }
                        EntryTrackingAccountOperationResult.Completed -> Unit
                        is EntryTrackingAccountOperationResult.Failed -> {
                            withUIContext { context.toast(result.cause.message.toString()) }
                        }
                        is EntryTrackingAccountOperationResult.Unavailable -> Unit
                    }
                }
            }
        }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = trackPreferences.autoUpdateTrack,
                title = stringResource(MR.strings.pref_auto_update_manga_sync),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = trackPreferences.autoUpdateTrackOnMarkRead,
                entries = AutoTrackState.entries
                    .associateWith { stringResource(it.titleRes) },
                title = stringResource(MR.strings.pref_auto_update_manga_on_mark_read),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.services),
                preferenceItems = regularAccounts.map { account ->
                    Preference.PreferenceItem.TrackingAccountPreference(
                        account = account,
                        isProfileSpecific = true,
                        login = { beginLogin(account) },
                        logout = { dialog = LogoutDialog(account) },
                    )
                } + Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.tracking_info)),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.enhanced_services),
                preferenceItems = (
                    availableEnhancedAccounts
                        .map { account ->
                            Preference.PreferenceItem.TrackingAccountPreference(
                                account = account,
                                isProfileSpecific = true,
                                login = { beginLogin(account) },
                                logout = {
                                    scope.launchIO { trackingFeature.logout(account.service.id) }
                                },
                            )
                        } + listOf(Preference.PreferenceItem.InfoPreference(enhancedTrackerInfo))
                    ),
            ),
        )
    }

    @Composable
    private fun TrackingLoginDialog(
        account: EntryTrackingAccount,
        storedUsername: String,
        storedPassword: String,
        trackingFeature: EntryTrackingFeature,
        onDismissRequest: () -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var username by remember { mutableStateOf(TextFieldValue(storedUsername)) }
        var password by remember { mutableStateOf(TextFieldValue(storedPassword)) }
        var processing by remember { mutableStateOf(false) }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(MR.strings.login_title, account.service.name),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(MR.strings.action_close),
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.Username + ContentType.EmailAddress },
                        value = username,
                        onValueChange = { username = it },
                        label = {
                            val label = when (
                                (account.loginMethod as EntryTrackingLoginMethod.Credentials).identity
                            ) {
                                EntryTrackingCredentialIdentity.USERNAME -> MR.strings.username
                                EntryTrackingCredentialIdentity.EMAIL -> MR.strings.email
                            }
                            Text(text = stringResource(label))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        isError = inputError && !processing,
                    )

                    var hidePassword by remember { mutableStateOf(true) }
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.Password },
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(text = stringResource(MR.strings.password)) },
                        trailingIcon = {
                            IconButton(onClick = { hidePassword = !hidePassword }) {
                                Icon(
                                    imageVector = if (hidePassword) {
                                        Icons.Filled.Visibility
                                    } else {
                                        Icons.Filled.VisibilityOff
                                    },
                                    contentDescription = null,
                                )
                            }
                        },
                        visualTransformation = if (hidePassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        singleLine = true,
                        isError = inputError && !processing,
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !processing && username.text.isNotBlank() && password.text.isNotBlank(),
                    onClick = {
                        scope.launchIO {
                            processing = true
                            val result = trackingFeature.loginWithCredentials(
                                serviceId = account.service.id,
                                username = username.text,
                                password = password.text,
                            )
                            inputError = result !is EntryTrackingAccountOperationResult.Completed
                            when (result) {
                                EntryTrackingAccountOperationResult.Completed -> withUIContext {
                                    context.toast(MR.strings.login_success)
                                    onDismissRequest()
                                }
                                is EntryTrackingAccountOperationResult.Failed -> withUIContext {
                                    context.toast(result.cause.message.toString())
                                }
                                is EntryTrackingAccountOperationResult.ExternalAuthorization,
                                is EntryTrackingAccountOperationResult.Unavailable,
                                -> Unit
                            }
                            processing = false
                        }
                    },
                ) {
                    val id = if (processing) MR.strings.logging_in else MR.strings.login
                    Text(text = stringResource(id))
                }
            },
        )
    }

    @Composable
    private fun TrackingLogoutDialog(
        account: EntryTrackingAccount,
        trackingFeature: EntryTrackingFeature,
        onDismissRequest: () -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(MR.strings.logout_title, account.service.name),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest,
                    ) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launchIO {
                                when (val result = trackingFeature.logout(account.service.id)) {
                                    EntryTrackingAccountOperationResult.Completed -> withUIContext {
                                        onDismissRequest()
                                        context.toast(MR.strings.logout_success)
                                    }
                                    is EntryTrackingAccountOperationResult.Failed -> withUIContext {
                                        context.toast(result.cause.message.toString())
                                    }
                                    is EntryTrackingAccountOperationResult.ExternalAuthorization,
                                    is EntryTrackingAccountOperationResult.Unavailable,
                                    -> Unit
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(text = stringResource(MR.strings.logout))
                    }
                }
            },
        )
    }
}

private data class LoginDialog(
    val account: EntryTrackingAccount,
    val storedUsername: String,
    val storedPassword: String,
)

private data class LogoutDialog(
    val account: EntryTrackingAccount,
)
