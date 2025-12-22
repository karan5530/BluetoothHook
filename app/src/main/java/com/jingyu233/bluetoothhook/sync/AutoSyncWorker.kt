package com.jingyu233.bluetoothhook.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jingyu233.bluetoothhook.data.bridge.ConfigBridge
import com.jingyu233.bluetoothhook.data.local.SettingsDataStore
import com.jingyu233.bluetoothhook.data.local.VirtualDeviceDatabase
import com.jingyu233.bluetoothhook.data.repository.VirtualDeviceRepository
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.coroutines.flow.first

/**
 * WebDAV自动同步Worker
 * 定期执行WebDAV同步任务
 */
class AutoSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = Logger.Tags.SYNC
        const val WORK_NAME = "webdav_auto_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Logger.App.d(TAG, "AutoSyncWorker started")

            // 获取设置
            val settingsDataStore = SettingsDataStore(applicationContext)
            val settings = settingsDataStore.settingsFlow.first()

            // 检查自动同步是否启用
            if (!settings.autoSyncEnabled) {
                Logger.App.d(TAG, "Auto sync is disabled, skipping")
                return Result.success()
            }

            // 检查WebDAV配置
            if (settings.webdavUrl.isBlank()) {
                Logger.App.w(TAG, "WebDAV URL is empty, skipping sync")
                return Result.failure()
            }

            // 执行同步
            val database = VirtualDeviceDatabase.getInstance(applicationContext)
            val repository = VirtualDeviceRepository(database.virtualDeviceDao(), applicationContext)
            val client = WebDavClient(
                url = settings.webdavUrl,
                username = settings.webdavUsername,
                password = settings.webdavPassword
            )

            val syncManager = SyncManager(repository, client)
            val result = syncManager.syncNow(ConflictStrategy.MERGE_BY_TIMESTAMP)

            if (result.isSuccess) {
                val report = result.getOrThrow()
                Logger.App.i(TAG, "Auto sync completed: $report")

                // 通知Hook进程更新
                val configBridge = ConfigBridge(applicationContext)
                val devices = repository.getAllDevicesSnapshot()
                configBridge.writeDeviceConfig(devices)

                Result.success()
            } else {
                val error = result.exceptionOrNull()
                Logger.App.e(TAG, "Auto sync failed", error)
                Result.retry()
            }

        } catch (e: Exception) {
            Logger.App.e(TAG, "AutoSyncWorker error", e)
            Result.retry()
        }
    }
}
