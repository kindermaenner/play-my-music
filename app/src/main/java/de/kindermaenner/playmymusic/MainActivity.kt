package de.kindermaenner.playmymusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import de.kindermaenner.playmymusic.data.AppLocalTrackResolver
import de.kindermaenner.playmymusic.data.mapping.JsonMappingRepository
import de.kindermaenner.playmymusic.domain.usecase.IdentifyTrackUseCase
import de.kindermaenner.playmymusic.domain.usecase.ResolveTrackUriUseCase
import de.kindermaenner.playmymusic.mapping.AssetMappingLoader
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.settings.SettingsRepository
import de.kindermaenner.playmymusic.ui.main.MainViewModel
import de.kindermaenner.playmymusic.ui.navigation.AppNavigation
import de.kindermaenner.playmymusic.ui.theme.PlayMyMusicTheme

class MainActivity : ComponentActivity() {

    private val settingsRepo by lazy { SettingsRepository(this) }
    private val offlineSyncService by lazy { OfflineLibrarySyncService(this) }
    private val mappingRepository by lazy { JsonMappingRepository() }
    private val assetMappingLoader by lazy { AssetMappingLoader(this) }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                lateinit var vm: MainViewModel
                val localResolver = AppLocalTrackResolver(
                    storage = de.kindermaenner.playmymusic.offline.OfflineLibraryStorage(this@MainActivity),
                    settingsProvider = { vm.appSettings.value }
                )
                
                vm = MainViewModel(
                    repo = settingsRepo,
                    offlineSyncService = offlineSyncService,
                    mappingRepository = mappingRepository,
                    assetMappingLoader = assetMappingLoader,
                    identifyTrackUseCase = IdentifyTrackUseCase(mappingRepository),
                    resolveTrackUriUseCase = ResolveTrackUriUseCase(localResolver)
                )
                @Suppress("UNCHECKED_CAST")
                return vm as T
            }
        }
    }

    private var hasCameraPermission by mutableStateOf(false)
    private var isCameraAvailable by mutableStateOf(true)

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
        }

    private var hasNotificationPermission by mutableStateOf(false)

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasNotificationPermission = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCameraAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasCameraPermission && isCameraAvailable) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        renderApp()
    }

    private fun renderApp() {
        setContent {
            PlayMyMusicTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()
                val debugMode by viewModel.debugMode.collectAsState()
                val appSettings by viewModel.appSettings.collectAsState()
                val syncMessage by viewModel.syncMessage.collectAsState()
                val syncProgress by viewModel.syncProgress.collectAsState()
                val isSyncing by viewModel.isSyncing.collectAsState()

                AppNavigation(
                    navController = navController,
                    uiState = uiState,
                    debugMode = debugMode,
                    appSettings = appSettings,
                    syncMessage = syncMessage,
                    syncProgress = syncProgress,
                    isSyncing = isSyncing,
                    isCameraAvailable = isCameraAvailable,
                    cameraPermissionGranted = hasCameraPermission,
                    notificationPermissionGranted = hasNotificationPermission,
                    onQrScanned = { viewModel.onQrScanned(it) },
                    onPlayPauseToggled = { viewModel.onPlayPauseToggled(it) },
                    onPlaybackError = { viewModel.onPlaybackError(it) },
                    onResetToScanner = { viewModel.resetToScanner() },
                    onLoadMappings = { viewModel.loadMappings() },
                    onOpenSettings = { navController.navigate("settings") },
                    onDebugToggle = { viewModel.toggleDebugMode(it) },
                    onSettingsSave = { viewModel.saveSettings(it) },
                    onSyncMetadata = { viewModel.syncMetadata(this@MainActivity) },
                    onSyncTracks = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.syncTracks(this@MainActivity)
                    },
                    onRequestCameraPermission = {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }
}
