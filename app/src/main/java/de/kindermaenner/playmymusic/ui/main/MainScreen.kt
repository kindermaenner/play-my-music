package de.kindermaenner.playmymusic.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.compose.rememberAsyncImagePainter
import de.kindermaenner.playmymusic.camera.CameraPreview
import de.kindermaenner.playmymusic.core.mapping.MappingLoader
import de.kindermaenner.playmymusic.core.mapping.TrackPathResolver
import de.kindermaenner.playmymusic.core.model.Song
import de.kindermaenner.playmymusic.core.model.SongKey
import de.kindermaenner.playmymusic.core.qr.parseQr
import de.kindermaenner.playmymusic.mapping.AssetMappingLoader
import de.kindermaenner.playmymusic.offline.OfflineLibraryStorage
import de.kindermaenner.playmymusic.player.SmbDataSource
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.ui.state.stateId
import de.kindermaenner.playmymusic.ui.UiConstants
import de.kindermaenner.playmymusic.ui.components.NeonButton

@Composable
fun MainScreen(
    debugMode: Boolean,
    appSettings: AppSettings,
    onOpenSettings: () -> Unit
)  {

    val context = LocalContext.current
    val mappingLoader = remember(context) { AssetMappingLoader(context) }
    val offlineLibraryStorage = remember(context) { OfflineLibraryStorage(context) }
    val playbackSettings = appSettings.playback
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

    var isScanning by remember { mutableStateOf(true) }
    var lastSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var playbackPath by remember { mutableStateOf<String?>(null) }
    var playbackUri by remember { mutableStateOf<String?>(null) }
    var scanSession by remember { mutableStateOf(0) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                val source = playbackUri ?: "unbekannt"
                val cause = error.cause?.message
                val base = error.message ?: "Audiodatei konnte nicht abgespielt werden"
                playbackError = if (cause.isNullOrBlank()) {
                    "$base (Quelle: $source)"
                } else {
                    "$base: $cause (Quelle: $source)"
                }
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    fun togglePlayPause() {
        if (player.mediaItemCount == 0) {
            return
        }

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun playSong(song: Song) {
        val localTrackFile = offlineLibraryStorage.resolveLocalTrackFile(song, appSettings)
        if (localTrackFile != null) {
            playbackPath = localTrackFile.absolutePath
            playbackUri = localTrackFile.toURI().toString()
            playbackError = null
            player.setMediaItem(MediaItem.fromUri(playbackUri!!))
            player.prepare()
            player.play()
            return
        }

        val resolvedPath = TrackPathResolver.resolve(
            settings = playbackSettings,
            song = song,
            trackIdPadding = appSettings.trackIdPadding,
            trackFolderName = appSettings.trackFolderName
        )
        if (resolvedPath.isNullOrBlank()) {
            playbackPath = null
            playbackUri = null
            playbackError = "NAS-Konfiguration oder Mapping ist unvollständig"
            return
        }

        playbackPath = resolvedPath
        playbackError = null

        val resolvedUri = resolvedPath.toPlayableUriOrNull()
        if (resolvedUri == null) {
            player.stop()
            playbackUri = null
            playbackError = "Der konfigurierte NAS-Pfad konnte nicht in eine lesbare URI umgewandelt werden"
            return
        }

        playbackUri = resolvedUri

        player.setMediaItem(MediaItem.fromUri(resolvedUri))
        player.prepare()
        player.play()
    }

    // Ebene 1: Haupt-Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { stateId = "main_screen" }
            .testTag("state_main_screen")
            .background(UiConstants.Background)
    ) {
        // Titel oben
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

        // Quadratisches Vorschaufenster
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(UiConstants.Background)
                .border(3.dp, UiConstants.NeonGreen, RoundedCornerShape(12.dp))
        ) {
            if (lastSong == null) {
                key(scanSession) {
                    CameraPreview(
                        isActive = isScanning,
                        onValidResult = { qrValue ->
                            val parsedQrCode = parseQr(qrValue, appSettings.qrValidation)
                            if (parsedQrCode == null) {
                                playbackError = "Ungültiger QR-Code"
                                return@CameraPreview
                            }

                            val songs = MappingLoader.parse(
                                mappingLoader.loadJsonFiles(appSettings.mappingFilePostfix)
                            )
                            val song = songs[SongKey(urlPart = parsedQrCode.urlPart, id = parsedQrCode.trackId)]

                            if (song == null) {
                                playbackError = "Kein Mapping für den QR-Code gefunden"
                                return@CameraPreview
                            }

                            isScanning = false
                            lastSong = song
                            playSong(song)
                        }
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(
                        offlineLibraryStorage.resolveArtworkSource(lastSong!!) ?: ""
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                key(playbackPath) {
                    Box(modifier = Modifier.size(0.dp))
                }

                // Edition unten links
                Text(
                    text = lastSong!!.kuerzel ?: "",
                    color = UiConstants.NeonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(UiConstants.Background.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // ID unten rechts
                Text(
                    text = "%d".format(lastSong!!.id),
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

            // Play/Pause Overlay
            if ((lastSong != null) && (playbackError == null)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
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

        // Next card Button
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
                    lastSong = null
                    playbackError = null
                    playbackPath = null
                    playbackUri = null
                    scanSession += 1
                    isScanning = true
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Fehlertext
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (playbackError != null) {
                Text(
                    text = playbackError!!,
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }
    }

    // Ebene 2: Overlay (Info + Zahnrad)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Info-Fläche
        if (debugMode && (lastSong != null)) {
            Surface(
                tonalElevation = 4.dp,
                color = UiConstants.Surface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 56.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("hinterlegte Daten:", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- Titel: ${lastSong!!.title}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- Interpret: ${lastSong!!.artist}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- Jahr: ${lastSong!!.year}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- Datei: ${playbackPath ?: "nicht aufgelöst"}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                    Text("- URI: ${playbackUri ?: "nicht aufgelöst"}", color = UiConstants.NeonGreen, fontSize = 20.sp)
                }
            }
        }

        // Checkbox unten mittig
        /*Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showInfo,
                onCheckedChange = { showInfo = it }
            )
            Text("Info anzeigen", color = UiConstants.NeonGreen, fontSize = 20.sp)
        }*/

        // Zahnrad unten rechts
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

private fun String.toPlayableUriOrNull(): String? {
    val trimmedValue = trim()
    if (trimmedValue.isEmpty()) {
        return null
    }

    if (trimmedValue.contains("://")) {
        return trimmedValue.replace("\\", "/")
    }

    // Be tolerant to escaped input artifacts where \t, \n, ... might arrive as control chars.
    val normalizedBackslashes = trimmedValue
        .replace(Regex("[\\t\\n\\r\\u000C]"), "\\\\")
        .replace("/", "\\")

    val uncSegments = normalizedBackslashes
        .trimStart('\\')
        .split('\\')
        .filter { it.isNotBlank() }

    if (uncSegments.size < 2) {
        return null
    }

    // UNC with optional leading backslashes, e.g. \\host\share\folder\file.mp3.
    return "smb://${uncSegments.joinToString("/")}" 
}