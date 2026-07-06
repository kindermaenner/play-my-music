package de.kindermaenner.playmymusic.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import de.kindermaenner.playmymusic.ui.UiConstants
import de.kindermaenner.playmymusic.ui.state.stateId

@Composable
fun SettingsScreen(
    debugMode: Boolean,
    appSettings: AppSettings,
    syncMessage: String?,
    isSyncing: Boolean,
    onDebugToggle: (Boolean) -> Unit,
    onSave: (AppSettings) -> Unit,
    onSyncMetadata: () -> Unit,
    onSyncTracks: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var nasAddress by rememberSaveable { mutableStateOf(appSettings.playback.nasAddress) }
    var nasBaseFolder by rememberSaveable { mutableStateOf(appSettings.playback.nasBaseFolder) }
    var qrValidation by rememberSaveable { mutableStateOf(appSettings.qrValidation) }
    var mappingFilePostfix by rememberSaveable { mutableStateOf(appSettings.mappingFilePostfix) }
    var trackIdPadding by rememberSaveable { mutableStateOf(appSettings.trackIdPadding.toString()) }
    var trackFolderName by rememberSaveable { mutableStateOf(appSettings.trackFolderName) }
    var smbUsername by rememberSaveable { mutableStateOf(appSettings.smbAuth.username) }
    var smbPassword by rememberSaveable { mutableStateOf(appSettings.smbAuth.password) }
    var smbDomain by rememberSaveable { mutableStateOf(appSettings.smbAuth.domain) }

    LaunchedEffect(appSettings) {
        nasAddress = appSettings.playback.nasAddress
        nasBaseFolder = appSettings.playback.nasBaseFolder
        qrValidation = appSettings.qrValidation
        mappingFilePostfix = appSettings.mappingFilePostfix
        trackIdPadding = appSettings.trackIdPadding.toString()
        trackFolderName = appSettings.trackFolderName
        smbUsername = appSettings.smbAuth.username
        smbPassword = appSettings.smbAuth.password
        smbDomain = appSettings.smbAuth.domain
    }

    Column(Modifier
        .fillMaxSize()
        .semantics { stateId = "settings_screen" }
        .testTag("state_settings_screen")
        .background(UiConstants.Background)
        .verticalScroll(scrollState)
        .testTag("settings_screen_scroll")
        .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = debugMode,
                onCheckedChange = onDebugToggle,
                modifier = Modifier.testTag("debug_mode_checkbox"),
                colors = CheckboxDefaults.colors(
                    checkedColor = UiConstants.NeonGreen,
                    uncheckedColor = UiConstants.NeonGreen
                )
            )
            Text(
                "Debug‑Modus",
                color = UiConstants.NeonGreen,
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "NAS-Adresse:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = nasAddress,
            onValueChange = { nasAddress = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nas_address_field"),
            label = { Text("NAS address", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "NAS-Basisordner:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = nasBaseFolder,
            onValueChange = { nasBaseFolder = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nas_base_folder_field"),
            label = { Text("Base folder", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "QR-Validierungsstring:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = qrValidation,
            onValueChange = { qrValidation = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("qr_validation_field"),
            label = { Text("QR validation", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Mapping-Dateifilter (Postfix):",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = mappingFilePostfix,
            onValueChange = { mappingFilePostfix = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mapping_file_postfix_field"),
            label = { Text("Mapping file postfix", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Track-ID Padding:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = trackIdPadding,
            onValueChange = { trackIdPadding = it.filter(Char::isDigit) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("track_id_padding_field"),
            label = { Text("Track ID padding", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Track folder name:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = trackFolderName,
            onValueChange = { trackFolderName = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("track_folder_name_field"),
            label = { Text("Track folder", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "SMB Username:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = smbUsername,
            onValueChange = { smbUsername = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smb_username_field"),
            label = { Text("SMB username", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "SMB Password:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = smbPassword,
            onValueChange = { smbPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smb_password_field"),
            label = { Text("SMB password", color = UiConstants.NeonGreen) },
            visualTransformation = PasswordVisualTransformation(),
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "SMB Domain:",
            color = UiConstants.NeonGreen,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = smbDomain,
            onValueChange = { smbDomain = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smb_domain_field"),
            label = { Text("SMB domain", color = UiConstants.NeonGreen) },
            colors = neonTextFieldColors()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "App-Konfiguration kommt aus assets/settings/settings.json und optional assets/local/settings/settings.json.",
            color = UiConstants.NeonGreen,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Mappings werden aus assets/mappings und optional assets/local/mappings geladen.",
            color = UiConstants.NeonGreen,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(16.dp))

        if (syncMessage != null) {
            Text(
                text = syncMessage,
                modifier = Modifier.testTag("sync_message_text"),
                color = UiConstants.NeonGreen,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSyncMetadata,
                enabled = !isSyncing,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sync_metadata_button")
            ) {
                Text("Sync metadata")
            }

            Button(
                onClick = onSyncTracks,
                enabled = !isSyncing,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sync_tracks_button")
            ) {
                Text("Sync tracks")
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onSave(
                        AppSettings(
                            playback = PlaybackSettings(
                                nasAddress = nasAddress.trim(),
                                nasBaseFolder = nasBaseFolder.trim()
                            ),
                            qrValidation = qrValidation.trim(),
                            mappingFilePostfix = mappingFilePostfix.trim(),
                            trackIdPadding = trackIdPadding.toIntOrNull()?.coerceAtLeast(1)
                                ?: appSettings.trackIdPadding,
                            trackFolderName = trackFolderName.trim().ifBlank { appSettings.trackFolderName },
                            smbAuth = SmbAuthSettings(
                                username = smbUsername.trim(),
                                password = smbPassword,
                                domain = smbDomain.trim()
                            )
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_button")
            ) {
                Text("Save")
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .testTag("back_button")
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun neonTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = UiConstants.NeonGreen,
    unfocusedTextColor = UiConstants.NeonGreen,
    focusedBorderColor = UiConstants.NeonGreen,
    unfocusedBorderColor = UiConstants.NeonGreen,
    focusedLabelColor = UiConstants.NeonGreen,
    unfocusedLabelColor = UiConstants.NeonGreen,
    cursorColor = UiConstants.NeonGreen,
    focusedContainerColor = UiConstants.Surface,
    unfocusedContainerColor = UiConstants.Surface
)