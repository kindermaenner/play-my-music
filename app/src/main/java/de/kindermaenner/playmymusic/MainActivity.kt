package de.kindermaenner.playmymusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.settings.SettingsRepository
import de.kindermaenner.playmymusic.ui.main.MainViewModel
import de.kindermaenner.playmymusic.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    private val repo by lazy { SettingsRepository(this) }
    private val offlineSyncService by lazy { OfflineLibrarySyncService(this) }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repo, offlineSyncService) as T
            }
        }
    }

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
            val debugMode by viewModel.debugMode.collectAsState()
            val appSettings by viewModel.appSettings.collectAsState()
            val syncMessage by viewModel.syncMessage.collectAsState()
            val isSyncing by viewModel.isSyncing.collectAsState()

            AppNavigation(
                navController = navController,
                debugMode = debugMode,
                appSettings = appSettings,
                syncMessage = syncMessage,
                isSyncing = isSyncing,
                onOpenSettings = { navController.navigate("settings") },
                onDebugToggle = { viewModel.toggleDebugMode(it) },
                onSettingsSave = { viewModel.saveSettings(it) },
                onSyncMetadata = { viewModel.syncMetadata() },
                onSyncTracks = { viewModel.syncTracks() }
            )
        }
    }
}
