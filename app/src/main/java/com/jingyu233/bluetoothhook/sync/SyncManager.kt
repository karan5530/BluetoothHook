package com.jingyu233.bluetoothhook.sync

import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import com.jingyu233.bluetoothhook.data.repository.VirtualDeviceRepository
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 同步管理器
 * 协调本地数据库和 WebDAV 服务器之间的同步
 */
class SyncManager(
    private val repository: VirtualDeviceRepository,
    private val webDavClient: WebDavClient
) {

    companion object {
        private val TAG = Logger.Tags.DATA_REPOSITORY
    }

    /**
     * 立即同步
     * @param strategy 冲突解决策略
     * @return 同步报告
     */
    suspend fun syncNow(strategy: ConflictStrategy): Result<SyncReport> = withContext(Dispatchers.IO) {
        try {
            Logger.App.i(TAG, "Starting sync with strategy: $strategy")

            // 获取本地设备
            val localDevices = repository.getAllDevicesSnapshot()
            Logger.App.d(TAG, "Local devices: ${localDevices.size}")

            // 获取远程设备
            val remoteResult = webDavClient.downloadDevices()
            if (remoteResult.isFailure) {
                return@withContext Result.failure(remoteResult.exceptionOrNull()!!)
            }
            val remoteDevices = remoteResult.getOrThrow()
            Logger.App.d(TAG, "Remote devices: ${remoteDevices.size}")

            // 根据策略合并数据
            val mergedDevices = when (strategy) {
                ConflictStrategy.LOCAL_WINS -> {
                    Logger.App.i(TAG, "Using LOCAL_WINS strategy")
                    localDevices
                }
                ConflictStrategy.REMOTE_WINS -> {
                    Logger.App.i(TAG, "Using REMOTE_WINS strategy")
                    remoteDevices
                }
                ConflictStrategy.MERGE_BY_TIMESTAMP -> {
                    Logger.App.i(TAG, "Using MERGE_BY_TIMESTAMP strategy")
                    mergeByTimestamp(localDevices, remoteDevices)
                }
            }

            // 更新本地数据库
            repository.deleteAllDevices()
            repository.addDevices(mergedDevices)

            // 上传到远程
            val uploadResult = webDavClient.uploadDevices(mergedDevices)
            if (uploadResult.isFailure) {
                return@withContext Result.failure(uploadResult.exceptionOrNull()!!)
            }

            // 通知 Hook 进程
            repository.notifyHookProcess()

            // 计算同步报告
            val report = calculateReport(localDevices, remoteDevices, mergedDevices, strategy)
            Logger.App.i(TAG, "Sync completed: $report")

            Result.success(report)

        } catch (e: Exception) {
            Logger.App.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * 按时间戳合并设备列表
     * 对于相同 ID 的设备，保留 updatedAt 更新的那个
     * 对于不同 ID 的设备，全部保留
     */
    private fun mergeByTimestamp(
        localDevices: List<VirtualDevice>,
        remoteDevices: List<VirtualDevice>
    ): List<VirtualDevice> {
        val deviceMap = mutableMapOf<String, VirtualDevice>()

        // 先添加本地设备
        localDevices.forEach { device ->
            deviceMap[device.id] = device
        }

        // 然后处理远程设备
        remoteDevices.forEach { remoteDevice ->
            val localDevice = deviceMap[remoteDevice.id]

            if (localDevice == null) {
                // 远程独有的设备，直接添加
                deviceMap[remoteDevice.id] = remoteDevice
            } else {
                // ID 冲突，比较时间戳
                if (remoteDevice.updatedAt > localDevice.updatedAt) {
                    Logger.App.d(TAG, "Merge: Remote device ${remoteDevice.name} is newer")
                    deviceMap[remoteDevice.id] = remoteDevice
                } else {
                    Logger.App.d(TAG, "Merge: Local device ${localDevice.name} is newer")
                    // 保持本地版本
                }
            }
        }

        return deviceMap.values.toList()
    }

    /**
     * 计算同步报告
     */
    private fun calculateReport(
        localDevices: List<VirtualDevice>,
        remoteDevices: List<VirtualDevice>,
        mergedDevices: List<VirtualDevice>,
        strategy: ConflictStrategy
    ): SyncReport {
        val localIds = localDevices.map { it.id }.toSet()
        val remoteIds = remoteDevices.map { it.id }.toSet()
        val mergedIds = mergedDevices.map { it.id }.toSet()

        val added = mergedIds - localIds
        val deleted = localIds - mergedIds
        val updated = localIds.intersect(mergedIds)

        val conflicts = when (strategy) {
            ConflictStrategy.MERGE_BY_TIMESTAMP -> {
                // 计算实际发生的冲突（相同 ID 但内容不同）
                localIds.intersect(remoteIds).count { id ->
                    val local = localDevices.find { it.id == id }
                    val remote = remoteDevices.find { it.id == id }
                    local != remote
                }
            }
            else -> 0
        }

        return SyncReport(
            localDevices = localDevices.size,
            remoteDevices = remoteDevices.size,
            mergedDevices = mergedDevices.size,
            added = added.size,
            updated = updated.size,
            deleted = deleted.size,
            conflicts = conflicts,
            strategy = strategy
        )
    }
}

/**
 * 冲突解决策略
 */
enum class ConflictStrategy {
    LOCAL_WINS,           // 本地优先（上传到远程）
    REMOTE_WINS,          // 远程优先（下载覆盖本地）
    MERGE_BY_TIMESTAMP    // 按时间戳合并（保留最新的）
}

/**
 * 同步报告
 */
data class SyncReport(
    val localDevices: Int,      // 同步前本地设备数
    val remoteDevices: Int,     // 同步前远程设备数
    val mergedDevices: Int,     // 同步后设备数
    val added: Int,             // 新增设备数
    val updated: Int,           // 更新设备数
    val deleted: Int,           // 删除设备数
    val conflicts: Int,         // 冲突数量
    val strategy: ConflictStrategy  // 使用的策略
) {
    override fun toString(): String {
        return "本地:$localDevices, 远程:$remoteDevices → 合并:$mergedDevices (新增:$added, 更新:$updated, 删除:$deleted, 冲突:$conflicts)"
    }
}
