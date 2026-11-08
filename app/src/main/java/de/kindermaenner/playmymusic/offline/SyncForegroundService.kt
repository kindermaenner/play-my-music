package de.kindermaenner.playmymusic.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import de.kindermaenner.playmymusic.R
import de.kindermaenner.playmymusic.settings.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SyncForegroundService : Service() {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val serviceScope by lazy { CoroutineScope(ioDispatcher + SupervisorJob()) }
    internal var syncService: OfflineLibrarySyncService? = null

    override fun onCreate() {
        super.onCreate()
        if (syncService == null) {
            syncService = OfflineLibrarySyncService(this)
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settingsJson = intent?.getStringExtra(EXTRA_SETTINGS_JSON)
        val includeTracks = intent?.getBooleanExtra(EXTRA_INCLUDE_TRACKS, true) ?: true

        val settings = if (!settingsJson.isNullOrBlank()) {
            Gson().fromJson(settingsJson, AppSettings::class.java)
        } else {
            AppSettings()
        }

        val initialNotification = buildNotification("Starte Synchronisation...", 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        OfflineSyncManager.setSyncing(true)
        OfflineSyncManager.clearMessage()

        serviceScope.launch {
            val service = syncService ?: OfflineLibrarySyncService(this@SyncForegroundService)
            val result = runCatching {
                if (includeTracks) {
                    service.syncTracks(settings) { progress ->
                        OfflineSyncManager.updateProgress(progress)
                        val notification = buildNotification(
                            content = "${progress.current} / ${progress.total} Tracks (${progress.currentItem})",
                            current = progress.current,
                            total = progress.total
                        )
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                } else {
                    service.syncMetadata(settings)
                }
            }

            OfflineSyncManager.setSyncing(false)
            OfflineSyncManager.updateProgress(null)

            result.fold(
                onSuccess = { res ->
                    OfflineSyncManager.setMessage(res.message)
                },
                onFailure = { err ->
                    OfflineSyncManager.setMessage(err.message ?: "Synchronisation fehlgeschlagen")
                }
            )

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        OfflineSyncManager.setSyncing(false)
        OfflineSyncManager.updateProgress(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(content: String, current: Int, total: Int): android.app.Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Play My Music: Offline-Sync")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (total > 0) {
            builder.setProgress(total, current, false)
        } else {
            builder.setProgress(0, 0, false)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Offline Synchronisation",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Fortschritt der Offline-Musik-Synchronisation"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "offline_sync_channel"
        const val NOTIFICATION_ID = 4201
        const val EXTRA_SETTINGS_JSON = "extra_settings_json"
        const val EXTRA_INCLUDE_TRACKS = "extra_include_tracks"

        fun startSync(context: Context, settings: AppSettings, includeTracks: Boolean) {
            val intent = Intent(context, SyncForegroundService::class.java).apply {
                putExtra(EXTRA_SETTINGS_JSON, Gson().toJson(settings))
                putExtra(EXTRA_INCLUDE_TRACKS, includeTracks)
            }
            context.startForegroundService(intent)
        }
    }
}
