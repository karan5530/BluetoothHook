package com.jingyu233.bluetoothhook.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jingyu233.bluetoothhook.MainActivity
import com.jingyu233.bluetoothhook.R
import com.jingyu233.bluetoothhook.data.bridge.ConfigBridge
import com.jingyu233.bluetoothhook.data.local.SettingsDataStore
import com.jingyu233.bluetoothhook.data.local.VirtualDeviceDatabase
import com.jingyu233.bluetoothhook.data.repository.VirtualDeviceRepository
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * WebDAV自动同步前台服务
 * 支持短间隔(小于15分钟)的连续同步
 */
class AutoSyncService : Service() {

    companion object {
        private val TAG = Logger.Tags.SYNC
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_sync_channel"
        private const val CHANNEL_NAME = "自动同步服务"

        const val ACTION_START_SYNC = "com.jingyu233.bluetoothhook.ACTION_START_SYNC"
        const val ACTION_STOP_SYNC = "com.jingyu233.bluetoothhook.ACTION_STOP_SYNC"

        fun startService(context: Context) {
            val intent = Intent(context, AutoSyncService::class.java).apply {
                action = ACTION_START_SYNC
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AutoSyncService::class.java).apply {
                action = ACTION_STOP_SYNC
            }
            context.stopService(intent)
        }
    }

    private lateinit var repository: VirtualDeviceRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var configBridge: ConfigBridge

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null

    private var syncCount = 0
    private var lastSyncTime = 0L

    override fun onCreate() {
        super.onCreate()

        val database = VirtualDeviceDatabase.getInstance(applicationContext)
        repository = VirtualDeviceRepository(database.virtualDeviceDao(), applicationContext)
        settingsDataStore = SettingsDataStore(applicationContext)
        configBridge = ConfigBridge(applicationContext)

        createNotificationChannel()
        Logger.App.d(TAG, "AutoSyncService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SYNC -> {
                Logger.App.i(TAG, "Starting auto-sync service")
                startForeground(NOTIFICATION_ID, createNotification())
                startSyncLoop()
            }
            ACTION_STOP_SYNC -> {
                Logger.App.i(TAG, "Stopping auto-sync service")
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        serviceScope.cancel()
        Logger.App.d(TAG, "AutoSyncService destroyed, total syncs: $syncCount")
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            try {
                while (isActive) {
                    val settings = settingsDataStore.settingsFlow.first()

                    if (!settings.autoSyncEnabled) {
                        Logger.App.d(TAG, "Auto sync disabled, stopping service")
                        stopSelf()
                        return@launch
                    }

                    if (settings.webdavUrl.isBlank()) {
                        Logger.App.w(TAG, "WebDAV URL is empty")
                        delay(settings.syncIntervalSeconds * 1000L)
                        continue
                    }

                    // 执行同步
                    performSync(settings.webdavUrl, settings.webdavUsername, settings.webdavPassword)

                    // 更新通知
                    updateNotification()

                    // 等待下一次同步
                    delay(settings.syncIntervalSeconds * 1000L)
                }
            } catch (e: Exception) {
                Logger.App.e(TAG, "Sync loop error", e)
            }
        }
    }

    private suspend fun performSync(url: String, username: String, password: String) {
        try {
            val startTime = System.currentTimeMillis()
            Logger.App.d(TAG, "Starting sync #${syncCount + 1}")

            val client = WebDavClient(url, username, password)
            val syncManager = SyncManager(repository, client)
            val result = syncManager.syncNow(ConflictStrategy.MERGE_BY_TIMESTAMP)

            if (result.isSuccess) {
                val report = result.getOrThrow()
                syncCount++
                lastSyncTime = System.currentTimeMillis()
                val duration = lastSyncTime - startTime

                Logger.App.i(TAG, "Sync #$syncCount completed in ${duration}ms: $report")

                // 通知Hook进程更新
                val devices = repository.getAllDevicesSnapshot()
                configBridge.writeDeviceConfig(devices)
            } else {
                val error = result.exceptionOrNull()
                Logger.App.e(TAG, "Sync failed", error)
            }
        } catch (e: Exception) {
            Logger.App.e(TAG, "Sync error", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WebDAV自动同步后台服务"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WebDAV 自动同步")
            .setContentText("同步服务运行中...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WebDAV 自动同步")
            .setContentText("已同步 $syncCount 次")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
