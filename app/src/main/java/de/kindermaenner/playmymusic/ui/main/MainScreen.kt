package de.kindermaenner.playmymusic.ui.main

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.compose.rememberAsyncImagePainter
import de.kindermaenner.playmymusic.camera.CameraPreview
import de.kindermaenner.playmymusic.offline.OfflineLibraryStorage
import de.kindermaenner.playmymusic.player.SmbDataSource
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.ui.UiConstants
import de.kindermaenner.playmymusic.ui.components.NeonButton
import de.kindermaenner.playmymusic.ui.state.stateId

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    debugMode: Boolean,
    appSettings: AppSettings,
    isCameraAvailable: Boolean,
    cameraPermissionGranted: Boolean,
    onQrScanned: (String) -> Unit,
    onPlayPauseToggled: (Boolean) -> Unit,
    onPlaybackError: (String) -> Unit,
    onResetToScanner: () -> Unit,
    onLoadMappings: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestCameraPermission: () -> Unit
) {
    val context = LocalContext.current
    val offlineLibraryStorage = remember(context) { OfflineLibraryStorage(context) }
    
    val player = remember(context, appSettings.smbAuth.username, appSettings.smbAuth.password, appSettings.smbAuth.domain) {
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            SmbDataSource.Factory(
                smbUsername = appSettings.smbAuth.username,
                smbPassword = appSettings.smbAuth.password,
                smbDomain = appSettings.smbAuth.domain
            )
        )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    LaunchedEffect(Unit) {
        onLoadMappings()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                onPlayPauseToggled(playing)
            }

            override fun onPlayerError(error: PlaybackException) {
                val source = uiState.playbackUri ?: "unknown"
                val cause = error.cause?.message
                val base = error.message ?: "Playback error"
                onPlaybackError(if (cause.isNullOrBlank()) "$base ($source)" else "$base: $cause ($source)")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(uiState.playbackUri) {
        uiState.playbackUri?.let { uri ->
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() {
        if (player.mediaItemCount == 0) return
        if (player.isPlaying) player.pause() else player.play()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { stateId = "main_screen" }
            .testTag("state_main_screen")
            .background(UiConstants.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Play My Music!",
                color = UiConstants.NeonGreen,
                fontSize = 48.sp,
                fontFamily = UiConstants.Audiowide
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(UiConstants.Background)
                .border(3.dp, UiConstants.NeonGreen, RoundedCornerShape(12.dp))
        ) {
            if (uiState.currentTrack == null) {
                if (!isCameraAvailable) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No camera hardware detected.\nCannot scan QR codes.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (!cameraPermissionGranted) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Camera permission required.",
                            color = Color.Red,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRequestCameraPermission) {
                            Text("Grant Permission")
                        }
                    }
                } else {
                    CameraPreview(
                        isActive = uiState.isScanning,
                        onValidResult = onQrScanned
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(
                        offlineLibraryStorage.resolveArtworkSource(uiState.currentTrack) ?: ""
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = uiState.currentTrack.editionAbbreviation,
                    color = UiConstants.NeonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(UiConstants.Background.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Text(
                    text = "%d".format(uiState.currentTrack.id),
                    color = UiConstants.NeonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(UiConstants.Background.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (uiState.currentTrack != null && uiState.error == null) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = UiConstants.NeonGreen,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(96.dp)
                        .clickable { togglePlayPause() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            NeonButton(
                text = "next card",
                onClick = {
                    player.stop()
                    player.clearMediaItems()
                    onResetToScanner()
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            uiState.error?.let {
                Text(text = it, color = Color.White, fontSize = 20.sp)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (debugMode && uiState.currentTrack != null) {
            Surface(
                tonalElevation = 4.dp,
                color = UiConstants.Surface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 56.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Track Info:", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- Title: ${uiState.currentTrack.title}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- Artist: ${uiState.currentTrack.artist}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- Year: ${uiState.currentTrack.year}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- URI: ${uiState.playbackUri ?: "not resolved"}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                }
            }
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = UiConstants.NeonGreen
            )
        }
    }
}
