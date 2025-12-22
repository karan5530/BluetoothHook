package com.jingyu233.bluetoothhook.hook

import android.bluetooth.BluetoothAdapter
import android.os.SystemClock
import com.jingyu233.bluetoothhook.utils.Logger
import de.robv.android.xposed.XposedHelpers

/**
 * ScanResult构造器
 * 根据逆向代码分析（ScanController.java line 431）构造符合系统要求的ScanResult对象
 */
class ScanResultBuilder(private val classLoader: ClassLoader) {

    companion object {
        private val TAG = Logger.Tags.HOOK_BUILDER
    }

    /**
     * 构造ScanResult对象
     *
     * @param macAddress MAC地址 (格式: AA:BB:CC:DD:EE:FF)
     * @param rssi 信号强度 (-100 to 0)
     * @param advDataHex 广播数据十六进制字符串
     * @param scanResponseHex 扫描响应数据十六进制字符串（可选）
     * @param useExtendedAdvertising 是否使用扩展广播
     * @param deviceName 设备名称（可选，会添加到广播数据中）
     * @return ScanResult对象，失败返回null
     */
    fun buildScanResult(
        macAddress: String,
        rssi: Int,
        advDataHex: String,
        scanResponseHex: String = "",
        useExtendedAdvertising: Boolean = false,
        deviceName: String? = null
    ): Any? {
        return try {
            // 1. 获取BluetoothAdapter并创建BluetoothDevice
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter.getRemoteDevice(macAddress)

            // 2. 合并广播数据和扫描响应数据
            val combinedDataHex = if (scanResponseHex.isNotEmpty()) {
                advDataHex + scanResponseHex
            } else {
                advDataHex
            }
            val advBytes = hexStringToByteArray(combinedDataHex)

            // 3. 使用ScanRecord.parseFromBytes解析广播数据
            val scanRecordClass = XposedHelpers.findClass(
                "android.bluetooth.le.ScanRecord",
                classLoader
            )
            val scanRecord = XposedHelpers.callStaticMethod(
                scanRecordClass,
                "parseFromBytes",
                advBytes
            )

            // 4. 构造ScanResult对象
            // 根据逆向代码line 431的构造器签名:
            // new ScanResult(device, eventType, primaryPhy, secondaryPhy,
            //               advertisingSid, txPower, rssi, periodicAdvInt,
            //               scanRecord, timestamp)
            val scanResultClass = XposedHelpers.findClass(
                "android.bluetooth.le.ScanResult",
                classLoader
            )

            val timestampNanos = SystemClock.elapsedRealtimeNanos()

            // 确定事件类型
            val eventType = if (useExtendedAdvertising) {
                0x00  // EXTENDED_ADVERTISING
            } else {
                0x10  // LEGACY_ADVERTISING
            }

            // 尝试使用完整参数的构造器
            try {
                XposedHelpers.newInstance(
                    scanResultClass,
                    device,                  // BluetoothDevice
                    eventType,               // eventType: 0x10 = LEGACY, 0x00 = EXTENDED
                    1,                       // primaryPhy: 1 = LE 1M
                    if (useExtendedAdvertising) 1 else 0,  // secondaryPhy: 1 = LE 1M, 0 = None
                    255,                     // advertisingSid: 255 = Not periodic
                    0,                       // txPower: 0 = Unknown
                    rssi,                    // rssi
                    0,                       // periodicAdvInt: 0 = None
                    scanRecord,              // ScanRecord
                    timestampNanos           // timestamp
                )
            } catch (e: Exception) {
                // 降级：使用简化构造器（兼容旧版本Android）
                XposedHelpers.newInstance(
                    scanResultClass,
                    device,
                    scanRecord,
                    rssi,
                    timestampNanos
                )
            }

        } catch (e: Throwable) {
            Logger.Hook.e(TAG, "Failed to build ScanResult for MAC=$macAddress", e)
            null
        }
    }

    /**
     * 十六进制字符串转字节数组
     */
    private fun hexStringToByteArray(hexString: String): ByteArray {
        val cleanHex = hexString.replace(" ", "").replace(":", "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) +
                    Character.digit(cleanHex[i + 1], 16)).toByte()
            i += 2
        }

        return data
    }

    /**
     * 生成标准BLE广播数据
     * 包含Flags和Complete Local Name
     */
    fun generateStandardAdvData(deviceName: String): String {
        val nameBytes = deviceName.toByteArray(Charsets.UTF_8)
        val nameLength = nameBytes.size

        // Flags AD结构 (3 bytes): 02 01 06
        // - Length: 0x02
        // - Type: 0x01 (Flags)
        // - Data: 0x06 (LE General Discoverable Mode, BR/EDR Not Supported)
        val flags = "020106"

        // Complete Local Name AD结构
        // - Length: nameLength + 1
        // - Type: 0x09 (Complete Local Name)
        // - Data: name bytes
        val nameHex = StringBuilder()
        nameHex.append(String.format("%02X", nameLength + 1)) // Length
        nameHex.append("09")                                   // Type
        nameBytes.forEach { byte ->
            nameHex.append(String.format("%02X", byte))
        }

        return flags + nameHex.toString()
    }

    /**
     * 验证MAC地址格式
     */
    fun isValidMacAddress(mac: String): Boolean {
        return mac.matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))
    }

    /**
     * 验证RSSI范围
     */
    fun isValidRssi(rssi: Int): Boolean {
        return rssi in -100..0
    }

    /**
     * 验证广播数据长度（Legacy BLE最多31字节，Extended最多254字节）
     */
    fun isValidAdvDataLength(hexString: String, extended: Boolean = false): Boolean {
        val byteCount = hexString.replace(" ", "").replace(":", "").length / 2
        return if (extended) {
            byteCount <= 254
        } else {
            byteCount <= 31
        }
    }
}
