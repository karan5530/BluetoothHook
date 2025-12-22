package com.jingyu233.bluetoothhook.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jingyu233.bluetoothhook.data.bridge.ConfigBridge
import com.jingyu233.bluetoothhook.data.local.SettingsDataStore
import com.jingyu233.bluetoothhook.data.local.VirtualDeviceDatabase
import com.jingyu233.bluetoothhook.data.model.AppSettings
import com.jingyu233.bluetoothhook.data.model.SyncLog
import com.jingyu233.bluetoothhook.data.repository.VirtualDeviceRepository
import com.jingyu233.bluetoothhook.sync.AutoSyncService
import com.jingyu233.bluetoothhook.sync.ConflictStrategy
import com.jingyu233.bluetoothhook.sync.SyncManager
import com.jingyu233.bluetoothhook.sync.WebDavClient
import com.jingyu233.bluetoothhook.util.JsonImportExport
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 设置页面ViewModel
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val TAG = Logger.Tags.UI_VM_SETTINGS
    }

    private val database = VirtualDeviceDatabase.getInstance(application)
    private val repository = VirtualDeviceRepository(database.virtualDeviceDao(), application)
    private val configBridge = ConfigBridge(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _hookStatus = MutableStateFlow("Unknown")
    val hookStatus: StateFlow<String> = _hookStatus.asStateFlow()

    private val _deviceCount = MutableStateFlow(0)
    val deviceCount: StateFlow<Int> = _deviceCount.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    // 同步日志(最多保存3条)
    private val _syncLogs = MutableStateFlow<List<SyncLog>>(emptyList())
    val syncLogs: StateFlow<List<SyncLog>> = _syncLogs.asStateFlow()

    init {
        loadSettings()
        refreshHookStatus()
        loadDeviceCount()
    }

    private fun loadSettings() {
        // 从DataStore加载设置
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _settings.value = settings
                Logger.App.d(TAG, "Loaded settings from DataStore")
            }
        }
    }

    private fun loadDeviceCount() {
        viewModelScope.launch {
            try {
                _deviceCount.value = repository.getDeviceCount()
                Logger.App.d(TAG, "Loaded device count: ${_deviceCount.value}")
            } catch (e: Exception) {
                Logger.App.e(TAG, "Failed to load device count", e)
            }
        }
    }

    fun refreshHookStatus() {
        _hookStatus.value = configBridge.getHookStatus()
        Logger.App.d(TAG, "Refreshed hook status: ${_hookStatus.value}")
    }

    fun refreshDeviceCount() {
        viewModelScope.launch {
            try {
                _deviceCount.value = repository.getDeviceCount()
                Logger.App.d(TAG, "Refreshed device count: ${_deviceCount.value}")
            } catch (e: Exception) {
                Logger.App.e(TAG, "Failed to refresh device count", e)
            }
        }
    }

    fun updateWebDavUrl(url: String) {
        _settings.value = _settings.value.copy(webdavUrl = url)
        viewModelScope.launch {
            settingsDataStore.updateWebDavUrl(url)
        }
    }

    fun updateWebDavUsername(username: String) {
        _settings.value = _settings.value.copy(webdavUsername = username)
        viewModelScope.launch {
            settingsDataStore.updateWebDavUsername(username)
        }
    }

    fun updateWebDavPassword(password: String) {
        _settings.value = _settings.value.copy(webdavPassword = password)
        viewModelScope.launch {
            settingsDataStore.updateWebDavPassword(password)
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoSyncEnabled = enabled)
        viewModelScope.launch {
            settingsDataStore.toggleAutoSync(enabled)

            // 根据开关状态启动或停止自动同步服务
            if (enabled) {
                AutoSyncService.startService(getApplication())
                Logger.App.i(TAG, "Auto sync service started")
            } else {
                AutoSyncService.stopService(getApplication())
                Logger.App.i(TAG, "Auto sync service stopped")
            }
        }
    }

    fun updateSyncInterval(seconds: Int) {
        _settings.value = _settings.value.copy(syncIntervalSeconds = seconds)
        viewModelScope.launch {
            settingsDataStore.updateSyncInterval(seconds)

            // 如果自动同步已启用,重启服务以应用新的间隔
            if (_settings.value.autoSyncEnabled) {
                AutoSyncService.stopService(getApplication())
                AutoSyncService.startService(getApplication())
                Logger.App.i(TAG, "Auto sync service restarted with new interval: $seconds seconds")
            }
        }
    }

    /**
     * 添加同步日志(最多保存3条)
     */
    private fun addSyncLog(success: Boolean, message: String, details: String = "") {
        val newLog = SyncLog(
            timestamp = System.currentTimeMillis(),
            success = success,
            message = message,
            details = details
        )
        _syncLogs.value = listOf(newLog) + _syncLogs.value.take(2) // 保持最新3条
        Logger.App.d(TAG, "Added sync log: $message")
    }

    fun testWebDavConnection() {
        _isTestingConnection.value = true
        _connectionTestResult.value = null

        viewModelScope.launch {
            try {
                val currentSettings = _settings.value

                if (currentSettings.webdavUrl.isBlank()) {
                    _connectionTestResult.value = "WebDAV URL cannot be empty"
                    return@launch
                }

                val client = WebDavClient(
                    url = currentSettings.webdavUrl,
                    username = currentSettings.webdavUsername,
                    password = currentSettings.webdavPassword
                )

                val result = client.testConnection()

                if (result.isSuccess) {
                    _connectionTestResult.value = "Connection successful"
                    Logger.App.i(TAG, "WebDAV connection test passed")
                } else {
                    val error = result.exceptionOrNull()
                    _connectionTestResult.value = "Connection failed: ${error?.message ?: "Unknown error"}"
                    Logger.App.e(TAG, "WebDAV connection test failed", error)
                }

            } catch (e: Exception) {
                _connectionTestResult.value = "Connection error: ${e.message}"
                Logger.App.e(TAG, "WebDAV connection test error", e)
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun syncNow(strategy: ConflictStrategy = ConflictStrategy.MERGE_BY_TIMESTAMP) {
        _isTestingConnection.value = true
        _connectionTestResult.value = "Syncing..."

        viewModelScope.launch {
            try {
                val currentSettings = _settings.value

                if (currentSettings.webdavUrl.isBlank()) {
                    _connectionTestResult.value = "WebDAV URL cannot be empty"
                    return@launch
                }

                val client = WebDavClient(
                    url = currentSettings.webdavUrl,
                    username = currentSettings.webdavUsername,
                    password = currentSettings.webdavPassword
                )

                val syncManager = SyncManager(repository, client)
                val result = syncManager.syncNow(strategy)

                if (result.isSuccess) {
                    val report = result.getOrThrow()
                    _connectionTestResult.value = "Sync successful! $report"
                    addSyncLog(true, "同步成功", report.toString())
                    Logger.App.i(TAG, "Sync completed: $report")
                    refreshDeviceCount()
                } else {
                    val error = result.exceptionOrNull()
                    val errorMsg = error?.message ?: "Unknown error"
                    _connectionTestResult.value = "Sync failed: $errorMsg"
                    addSyncLog(false, "同步失败", errorMsg)
                    Logger.App.e(TAG, "Sync failed", error)
                }

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                _connectionTestResult.value = "Sync error: $errorMsg"
                addSyncLog(false, "同步错误", errorMsg)
                Logger.App.e(TAG, "Sync error", e)
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun clearConnectionTestResult() {
        _connectionTestResult.value = null
    }

    // ==================== JSON 导入/导出 ====================

    private val _importExportStatus = MutableStateFlow<String?>(null)
    val importExportStatus: StateFlow<String?> = _importExportStatus.asStateFlow()

    /**
     * 导出设备到 JSON 文件
     * @param uri 用户选择的文件 URI
     * @return 导出的设备数量
     */
    fun exportDevices(uri: Uri?): Result<Int> {
        if (uri == null) {
            return Result.failure(IllegalArgumentException("未选择文件"))
        }

        return try {
            viewModelScope.launch {
                try {
                    // 获取所有设备
                    val devices = repository.getAllDevicesSnapshot()

                    if (devices.isEmpty()) {
                        _importExportStatus.value = "没有设备可导出"
                        return@launch
                    }

                    // 序列化为 JSON
                    val json = JsonImportExport.exportToJson(devices)

                    // 写入文件
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray())
                    } ?: throw IllegalStateException("无法打开输出流")

                    _importExportStatus.value = "成功导出 ${devices.size} 个设备"
                    Logger.App.i(TAG, "Exported ${devices.size} devices to $uri")

                } catch (e: Exception) {
                    _importExportStatus.value = "导出失败: ${e.message}"
                    Logger.App.e(TAG, "Export failed", e)
                }
            }
            Result.success(0)
        } catch (e: Exception) {
            Logger.App.e(TAG, "Export setup failed", e)
            Result.failure(e)
        }
    }

    /**
     * 从 JSON 文件导入设备
     * @param uri 用户选择的文件 URI
     * @param replaceExisting 是否替换现有设备（true=替换，false=追加）
     * @return 导入的设备数量
     */
    fun importDevices(uri: Uri?, replaceExisting: Boolean = false): Result<Int> {
        if (uri == null) {
            return Result.failure(IllegalArgumentException("未选择文件"))
        }

        return try {
            viewModelScope.launch {
                try {
                    // 读取文件内容
                    val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalStateException("无法读取文件")

                    // 解析 JSON
                    val importedDevices = JsonImportExport.importFromJson(json).getOrThrow()

                    if (importedDevices.isEmpty()) {
                        _importExportStatus.value = "文件中没有有效设备"
                        return@launch
                    }

                    // 获取现有设备 ID 集合
                    val existingIds = repository.getAllDevicesSnapshot().map { it.id }.toSet()

                    // 处理 ID 冲突：为重复的 ID 生成新 UUID
                    val devicesToImport = importedDevices.map { device ->
                        if (device.id in existingIds) {
                            Logger.App.d(TAG, "ID 冲突，为设备 ${device.name} 生成新 UUID")
                            device.copy(
                                id = UUID.randomUUID().toString(),
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            device.copy(
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                    }

                    // 导入设备
                    if (replaceExisting) {
                        repository.deleteAllDevices()
                        Logger.App.w(TAG, "清空现有设备")
                    }

                    repository.addDevices(devicesToImport)
                    repository.notifyHookProcess()

                    _importExportStatus.value = "成功导入 ${devicesToImport.size} 个设备"
                    Logger.App.i(TAG, "Imported ${devicesToImport.size} devices from $uri")

                    // 刷新设备计数
                    refreshDeviceCount()

                } catch (e: Exception) {
                    _importExportStatus.value = "导入失败: ${e.message}"
                    Logger.App.e(TAG, "Import failed", e)
                }
            }
            Result.success(0)
        } catch (e: Exception) {
            Logger.App.e(TAG, "Import setup failed", e)
            Result.failure(e)
        }
    }

    /**
     * 清除导入导出状态消息
     */
    fun clearImportExportStatus() {
        _importExportStatus.value = null
    }

    /**
     * 清理旧数据（删除所有设备并重新同步到Hook进程）
     */
    fun clearAllData() {
        viewModelScope.launch {
            try {
                _importExportStatus.value = "Clearing all data..."

                // 删除所有设备
                repository.deleteAllDevices()

                // 通知Hook进程更新
                repository.notifyHookProcess()

                // 刷新设备计数
                refreshDeviceCount()

                _importExportStatus.value = "All data cleared successfully"
                Logger.App.i(TAG, "All device data cleared")
            } catch (e: Exception) {
                _importExportStatus.value = "Failed to clear data: ${e.message}"
                Logger.App.e(TAG, "Failed to clear all data", e)
            }
        }
    }
}
