package com.jingyu233.bluetoothhook.data.repository

import android.content.Context
import com.jingyu233.bluetoothhook.data.bridge.ConfigBridge
import com.jingyu233.bluetoothhook.data.local.VirtualDeviceDao
import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 虚拟设备仓库
 * 提供数据访问的统一接口
 */
class VirtualDeviceRepository(
    private val deviceDao: VirtualDeviceDao,
    private val context: Context
) {

    companion object {
        private val TAG = Logger.Tags.DATA_REPOSITORY
    }

    private val configBridge = ConfigBridge(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allDevices: Flow<List<VirtualDevice>> = deviceDao.getAllDevices()
    val enabledDevices: Flow<List<VirtualDevice>> = deviceDao.getEnabledDevices()

    suspend fun getDeviceById(id: String): VirtualDevice? = withContext(Dispatchers.IO) {
        deviceDao.getDeviceById(id)
    }

    suspend fun getDeviceCount(): Int = withContext(Dispatchers.IO) {
        deviceDao.getDeviceCount()
    }

    suspend fun addDevice(device: VirtualDevice) = withContext(Dispatchers.IO) {
        Logger.App.d(TAG, "Adding device: ${device.name} (${device.id})")
        deviceDao.insertDevice(device)
        notifyHookProcess()
    }

    suspend fun addDevices(devices: List<VirtualDevice>) = withContext(Dispatchers.IO) {
        Logger.App.d(TAG, "Adding ${devices.size} devices")
        deviceDao.insertDevices(devices)
        notifyHookProcess()
    }

    suspend fun updateDevice(device: VirtualDevice) = withContext(Dispatchers.IO) {
        Logger.App.d(TAG, "Updating device: ${device.name} (${device.id})")
        deviceDao.updateDevice(device.copy(updatedAt = System.currentTimeMillis()))
        notifyHookProcess()
    }

    suspend fun toggleDevice(device: VirtualDevice) = withContext(Dispatchers.IO) {
        Logger.App.d(TAG, "Toggling device: ${device.name} enabled=${!device.enabled}")
        deviceDao.updateDeviceEnabled(device.id, !device.enabled)
        notifyHookProcess()
    }

    suspend fun deleteDevice(device: VirtualDevice) = withContext(Dispatchers.IO) {
        Logger.App.d(TAG, "Deleting device: ${device.name} (${device.id})")
        deviceDao.deleteDevice(device)
        notifyHookProcess()
    }

    suspend fun deleteAllDevices() = withContext(Dispatchers.IO) {
        Logger.App.w(TAG, "Deleting all devices")
        deviceDao.deleteAllDevices()
        notifyHookProcess()
    }

    /**
     * 获取所有设备的快照（非 Flow，用于导出等一次性操作）
     */
    suspend fun getAllDevicesSnapshot(): List<VirtualDevice> = withContext(Dispatchers.IO) {
        deviceDao.getAllDevicesSnapshot()
    }

    /**
     * 通知Hook进程重新加载配置
     * 将当前数据库中的所有设备同步到SharedPreferences供Hook进程读取
     */
    fun notifyHookProcess() {
        repositoryScope.launch {
            try {
                // 从数据库读取所有设备
                val devices = deviceDao.getAllDevicesSnapshot()

                // 写入到SharedPreferences
                configBridge.writeDeviceConfig(devices)

                Logger.App.d(TAG, "Synced ${devices.size} devices to ConfigBridge for Hook process")
            } catch (e: Exception) {
                Logger.App.e(TAG, "Failed to notify Hook process", e)
            }
        }
    }
}
