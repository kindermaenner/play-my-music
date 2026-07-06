package de.kindermaenner.playmymusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.offline.SyncResult
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SettingsRepository
import de.kindermaenner.playmymusic.ui.navigation.AppNavigation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repo by lazy { SettingsRepository(this) }
    private val offlineSyncService by lazy { OfflineLibrarySyncService(this) }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                renderApp()
            } else {
                setContent {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Camera permission required", color = Color.Red, fontSize = 24.sp)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            renderApp()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun renderApp() {
        setContent {
            val navController = rememberNavController()
            val debugMode by repo.debugMode.collectAsState(initial = false)
            val appSettings by repo.appSettings.collectAsState(
                initial = AppSettings()
            )
            var syncMessage by remember { mutableStateOf<String?>(null) }
            var isSyncing by remember { mutableStateOf(false) }

            AppNavigation(
                navController = navController,
                debugMode = debugMode,
                appSettings = appSettings,
                syncMessage = syncMessage,
                isSyncing = isSyncing,
                onOpenSettings = { navController.navigate("settings") },
                onDebugToggle = { value ->
                    lifecycleScope.launch { repo.setDebugMode(value) }
                },
                onSettingsSave = { value ->
                    lifecycleScope.launch { repo.setAppSettings(value) }
                },
                onSyncMetadata = {
                    lifecycleScope.launch {
                        isSyncing = true
                        syncMessage = runCatching {
                            offlineSyncService.syncMetadata(appSettings)
                        }.fold(
                            onSuccess = SyncResult::message,
                            onFailure = { it.message ?: "Metadata sync failed" }
                        )
                        isSyncing = false
                    }
                },
                onSyncTracks = {
                    lifecycleScope.launch {
                        isSyncing = true
                        syncMessage = runCatching {
                            offlineSyncService.syncTracks(appSettings)
                        }.fold(
                            onSuccess = SyncResult::message,
                            onFailure = { it.message ?: "Track sync failed" }
                        )
                        isSyncing = false
                    }
                }
            )
        }
    }
}
