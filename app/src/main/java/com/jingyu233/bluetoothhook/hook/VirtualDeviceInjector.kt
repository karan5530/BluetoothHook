package com.jingyu233.bluetoothhook.hook

import com.jingyu233.bluetoothhook.utils.Logger
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.concurrent.ConcurrentHashMap

/**
 * 虚拟设备注入器
 * 负责管理虚拟设备列表并将它们注入到扫描结果中
 */
class VirtualDeviceInjector(
    private val scanResultBuilder: ScanResultBuilder,
    private val prefs: XSharedPreferences
) {
    companion object {
        private val TAG = Logger.Tags.HOOK_INJECTOR
    }

    // 上次注入时间戳，用于控制注入频率
    private val lastInjectTime = ConcurrentHashMap<String, Long>()

    /**
     * 注入虚拟设备到扫描结果
     *
     * @param scanControllerInstance ScanController实例
     * @param scanManager ScanManager实例
     * @param scannerMap ScannerMap实例
     * @param scanQueue 当前扫描客户端队列
     */
    fun injectDevices(
        scanControllerInstance: Any,
        scanManager: Any,
        scannerMap: Any,
        scanQueue: Collection<*>
    ) {
        try {
            // 重新加载配置
            prefs.reload()

            // 读取虚拟设备列表
            val devicesJson = prefs.getString("devices", "[]") ?: "[]"
            if (devicesJson == "[]") {
                return // 没有配置虚拟设备，静默返回
            }

            // 解析虚拟设备列表
            val devices = try {
                Json.decodeFromString<List<VirtualDeviceData>>(devicesJson)
            } catch (e: Exception) {
                // 只在首次解析失败时输出错误，避免刷屏
                Logger.Hook.e(TAG, "JSON parse error: ${e.message}\nJSON: $devicesJson", e)
                return
            }

            // 过滤启用的设备
            val enabledDevices = devices.filter { it.enabled }
            if (enabledDevices.isEmpty()) {
                return // 没有启用的设备，静默返回
            }

            // 为每个虚拟设备生成扫描结果
            for (device in enabledDevices) {
                try {
                    injectSingleDevice(device, scanQueue, scannerMap)
                } catch (e: Throwable) {
                    Logger.Hook.e(TAG, "Failed to inject device ${device.name}", e)
                }
            }

        } catch (e: Throwable) {
            Logger.Hook.e(TAG, "Error in injectDevices", e)
        }
    }

    /**
     * 注入单个虚拟设备
     */
    private fun injectSingleDevice(
        device: VirtualDeviceData,
        scanQueue: Collection<*>,
        scannerMap: Any
    ) {
        // 检查注入频率限制
        val now = System.currentTimeMillis()
        val lastTime = lastInjectTime[device.id] ?: 0L
        if (now - lastTime < device.intervalMs) {
            return // 还没到注入间隔
        }

        // 更新注入时间
        lastInjectTime[device.id] = now

        // 验证设备参数
        if (!scanResultBuilder.isValidMacAddress(device.mac)) {
            Logger.Hook.w(TAG, "Invalid MAC address for device ${device.name}: ${device.mac}")
            return
        }

        if (!scanResultBuilder.isValidRssi(device.rssi)) {
            Logger.Hook.w(TAG, "Invalid RSSI for device ${device.name}: ${device.rssi}")
            return
        }

        // 构造ScanResult
        val scanResult = scanResultBuilder.buildScanResult(
            macAddress = device.mac,
            rssi = device.rssi,
            advDataHex = device.advDataHex,
            scanResponseHex = device.scanResponseHex,
            useExtendedAdvertising = device.useExtendedAdvertising,
            deviceName = device.name
        ) ?: return

        // 遍历所有扫描客户端，发送虚拟设备
        var deliveredCount = 0
        for (scanClient in scanQueue) {
            if (scanClient == null) continue
            try {
                deliverToClient(scanClient, scannerMap, scanResult)
                deliveredCount++
            } catch (e: Throwable) {
                // 客户端可能已断开，记录为debug级别
                Logger.Hook.d(TAG, "Failed to deliver to scan client: ${e.message}")
            }
        }

        if (deliveredCount > 0) {
            Logger.Hook.i(TAG, "Injected virtual device: ${device.name} (${device.mac}) to $deliveredCount clients")
        }
    }

    /**
     * 将ScanResult发送给扫描客户端
     * 根据逆向代码line 466: iScannerCallback.onScanResult(scanResult)
     */
    private fun deliverToClient(
        scanClient: Any,
        scannerMap: Any,
        scanResult: Any
    ) {
        try {
            // 获取scannerId
            val scannerId = XposedHelpers.getIntField(scanClient, "mScannerId")

            // 通过scannerMap获取ScannerApp
            val scannerApp = XposedHelpers.callMethod(scannerMap, "getById", scannerId)
                ?: return

            // 获取IScannerCallback
            val callback = XposedHelpers.getObjectField(scannerApp, "mCallback")
            if (callback == null) {
                // 有些客户端使用PendingIntent而不是callback
                return
            }

            // 调用callback.onScanResult(scanResult)
            XposedHelpers.callMethod(callback, "onScanResult", scanResult)

        } catch (e: Throwable) {
            // 某些客户端可能已断开连接，抛出异常让上层处理
            throw e
        }
    }
}

/**
 * 虚拟设备数据模型（用于从SharedPreferences反序列化）
 * 必须与VirtualDevice的JSON序列化格式匹配
 */
@Serializable
data class VirtualDeviceData(
    val id: String,
    val name: String,
    val mac: String,
    val rssi: Int,
    val advDataHex: String,
    val intervalMs: Long,
    val enabled: Boolean,
    val scanResponseHex: String = "",  // 扫描响应数据
    val useExtendedAdvertising: Boolean = false,  // 是否使用扩展广播
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
