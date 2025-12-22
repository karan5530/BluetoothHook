package com.jingyu233.bluetoothhook.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jingyu233.bluetoothhook.data.local.VirtualDeviceDatabase
import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import com.jingyu233.bluetoothhook.data.repository.VirtualDeviceRepository
import com.jingyu233.bluetoothhook.hook.ScanResultBuilder
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设备编辑器ViewModel
 */
class DeviceEditorViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        private val TAG = Logger.Tags.UI_VM_DEVICE_EDITOR
    }

    private val database = VirtualDeviceDatabase.getInstance(application)
    private val repository = VirtualDeviceRepository(database.virtualDeviceDao(), application)
    private val scanResultBuilder = ScanResultBuilder(javaClass.classLoader)

    private val deviceId: String? = savedStateHandle["deviceId"]

    private val _device = MutableStateFlow(createEmptyDevice())
    val device: StateFlow<VirtualDevice> = _device.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        if (deviceId != null && deviceId != "new") {
            loadDevice(deviceId)
        }
    }

    private fun loadDevice(id: String) {
        viewModelScope.launch {
            try {
                repository.getDeviceById(id)?.let { loadedDevice ->
                    _device.value = loadedDevice
                    Logger.App.d(TAG, "Loaded device: ${loadedDevice.name}")
                }
            } catch (e: Exception) {
                Logger.App.e(TAG, "Failed to load device with id: $id", e)
            }
        }
    }

    fun updateName(name: String) {
        _device.value = _device.value.copy(name = name)
        validateField("name", name)
    }

    fun updateMac(mac: String) {
        // 自动格式化MAC地址
        val formatted = formatMacAddress(mac)
        _device.value = _device.value.copy(mac = formatted)
        validateField("mac", formatted)
    }

    fun updateRssi(rssi: Int) {
        _device.value = _device.value.copy(rssi = rssi)
        validateField("rssi", rssi.toString())
    }

    fun updateAdvData(advData: String) {
        // 支持多种十六进制格式：0x前缀、空格、冒号、连字符分隔
        val normalized = normalizeHexInput(advData)
        _device.value = _device.value.copy(advDataHex = normalized)
        validateField("advData", normalized)
    }

    fun updateScanResponse(scanResponse: String) {
        // 支持多种十六进制格式：0x前缀、空格、冒号、连字符分隔
        val normalized = normalizeHexInput(scanResponse)
        _device.value = _device.value.copy(scanResponseHex = normalized)
        validateField("scanResponse", normalized)
    }

    /**
     * 切换扩展广播模式
     */
    fun toggleExtendedAdvertising() {
        val currentDevice = _device.value
        if (currentDevice.useExtendedAdvertising) {
            // 切换到传统模式，可能需要分割数据
            val totalBytes = currentDevice.getAdvDataByteLength()
            if (totalBytes <= 31) {
                _device.value = currentDevice.copy(
                    useExtendedAdvertising = false
                )
            } else if (totalBytes <= 62) {
                // 自动分割
                val (advPart, scanPart) = currentDevice.autoSplitAdvData()
                _device.value = currentDevice.copy(
                    advDataHex = advPart,
                    scanResponseHex = scanPart,
                    useExtendedAdvertising = false
                )
            } else {
                // 数据太大，无法切换到传统模式
                Logger.App.w(TAG, "Cannot switch to legacy mode: data too large (${totalBytes} bytes)")
            }
        } else {
            // 切换到扩展广播模式，合并所有数据到advDataHex
            val currentDevice = _device.value
            if (currentDevice.scanResponseHex.isNotEmpty()) {
                // 合并扫描响应到广播数据
                _device.value = currentDevice.copy(
                    advDataHex = currentDevice.advDataHex + currentDevice.scanResponseHex,
                    scanResponseHex = "",
                    useExtendedAdvertising = true
                )
            } else {
                _device.value = currentDevice.copy(
                    useExtendedAdvertising = true
                )
            }
        }
    }

    /**
     * 自动分割广播数据
     * 当数据超过31字节时，自动分割为adv + scan response 或启用扩展广播
     */
    fun autoSplitAdvData() {
        val currentDevice = _device.value
        val totalBytes = currentDevice.getAdvDataByteLength()

        if (totalBytes <= 31) {
            // 不需要分割
            return
        } else if (totalBytes <= 62) {
            // 分割为传统广播 + 扫描响应
            val (advPart, scanPart) = currentDevice.autoSplitAdvData()
            _device.value = currentDevice.copy(
                advDataHex = advPart,
                scanResponseHex = scanPart,
                useExtendedAdvertising = false
            )
            Logger.App.d(TAG, "Auto-split: adv=${advPart.length/2} bytes, scan=${scanPart.length/2} bytes")
        } else {
            // 使用扩展广播
            _device.value = currentDevice.copy(
                useExtendedAdvertising = true,
                scanResponseHex = ""
            )
            Logger.App.d(TAG, "Enabled extended advertising for ${totalBytes} bytes")
        }
    }

    /**
     * 标准化十六进制输入
     * 支持格式：
     * - 0x1A2B, 0X1a2b (0x前缀)
     * - 1A 2B 3C (空格分隔)
     * - 1A:2B:3C (冒号分隔)
     * - 1A-2B-3C (连字符分隔)
     * - 1A,2B,3C (逗号分隔)
     * - 混合格式：0x1A 2B:3C-4D
     */
    private fun normalizeHexInput(input: String): String {
        return input
            .replace(Regex("0[xX]"), "")              // 移除 0x/0X 前缀
            .replace(Regex("[\\s:,\\-_]"), "")        // 移除分隔符：空格、冒号、逗号、连字符、下划线
            .filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }  // 只保留十六进制字符
            .uppercase()                              // 转换为大写
    }

    fun updateInterval(interval: Long) {
        _device.value = _device.value.copy(intervalMs = interval)
    }

    fun saveDevice(onSuccess: () -> Unit) {
        if (!validate()) {
            return
        }

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val currentDevice = _device.value
                if (deviceId == null || deviceId == "new") {
                    repository.addDevice(currentDevice)
                    Logger.App.i(TAG, "Added new device: ${currentDevice.name}")
                } else {
                    repository.updateDevice(currentDevice)
                    Logger.App.i(TAG, "Updated device: ${currentDevice.name}")
                }
                onSuccess()
            } catch (e: Exception) {
                Logger.App.e(TAG, "Failed to save device", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val currentDevice = _device.value

        if (currentDevice.name.isBlank()) {
            errors["name"] = "设备名称不能为空"
        }

        if (!scanResultBuilder.isValidMacAddress(currentDevice.mac)) {
            errors["mac"] = "无效的MAC地址格式 (应为 AA:BB:CC:DD:EE:FF)"
        }

        if (!scanResultBuilder.isValidRssi(currentDevice.rssi)) {
            errors["rssi"] = "RSSI应在-100到0之间"
        }

        if (!currentDevice.advDataHex.matches(Regex("^[0-9A-Fa-f]*$"))) {
            errors["advData"] = "广播数据只能包含十六进制字符"
        }

        if (!currentDevice.scanResponseHex.matches(Regex("^[0-9A-Fa-f]*$"))) {
            errors["advData"] = "扫描响应数据只能包含十六进制字符"
        }

        // 根据模式验证数据长度
        if (!currentDevice.isValid()) {
            val advBytes = currentDevice.getAdvDataByteLength()
            val scanBytes = currentDevice.getScanResponseByteLength()

            errors["advData"] = when {
                currentDevice.useExtendedAdvertising && advBytes > 254 ->
                    "扩展广播数据过长 (最多254字节)"
                currentDevice.useExtendedAdvertising && scanBytes > 0 ->
                    "扩展广播模式不支持扫描响应数据"
                scanBytes > 0 && (advBytes > 31 || scanBytes > 31) ->
                    "传统广播最多31字节，扫描响应最多31字节"
                advBytes > 31 && scanBytes == 0 && !currentDevice.useExtendedAdvertising ->
                    "广播数据超过31字节，请使用自动分割或扩展广播"
                else ->
                    "广播数据无效"
            }
        }

        if (currentDevice.intervalMs < 100) {
            errors["interval"] = "间隔不能小于100ms"
        }

        _validationErrors.value = errors
        return errors.isEmpty()
    }

    private fun validateField(field: String, value: String) {
        val errors = _validationErrors.value.toMutableMap()
        errors.remove(field)
        _validationErrors.value = errors
    }

    private fun formatMacAddress(input: String): String {
        // 移除所有非十六进制字符
        val cleaned = input.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            .take(12)
            .uppercase()

        // 添加冒号分隔
        return cleaned.chunked(2).joinToString(":")
    }

    private fun createEmptyDevice(): VirtualDevice {
        return VirtualDevice(
            name = "",
            mac = "",
            rssi = -60,
            advDataHex = "020106", // 默认Flags
            intervalMs = 1000,
            enabled = true
        )
    }

    /**
     * 生成标准广播数据
     */
    fun generateStandardAdvData() {
        val name = _device.value.name
        if (name.isNotBlank()) {
            val advData = scanResultBuilder.generateStandardAdvData(name)
            _device.value = _device.value.copy(advDataHex = advData)
        }
    }
}
