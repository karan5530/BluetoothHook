package com.jingyu233.bluetoothhook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 虚拟蓝牙设备数据模型
 */
@Serializable
@Entity(tableName = "virtual_devices")
data class VirtualDevice(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,
    val mac: String,           // 格式: AA:BB:CC:DD:EE:FF
    val rssi: Int,             // 范围: -100 to 0
    val advDataHex: String,    // 十六进制字符串 (最多31字节 for legacy, 254字节 for extended)
    val intervalMs: Long,      // 广播间隔（毫秒）
    val enabled: Boolean = true,

    val scanResponseHex: String = "",  // 扫描响应数据 (最多31字节)
    val useExtendedAdvertising: Boolean = false,  // 是否使用扩展广播

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 验证设备数据是否有效
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                mac.matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")) &&
                rssi in -100..0 &&
                advDataHex.matches(Regex("^[0-9A-Fa-f]*$")) &&
                scanResponseHex.matches(Regex("^[0-9A-Fa-f]*$")) &&
                isAdvDataValid() &&
                intervalMs > 0
    }

    /**
     * 验证广播数据是否有效（根据模式）
     */
    private fun isAdvDataValid(): Boolean {
        val advBytes = getAdvDataByteLength()
        val scanBytes = getScanResponseByteLength()

        return when {
            useExtendedAdvertising -> advBytes <= 254 && scanBytes == 0
            scanBytes > 0 -> advBytes <= 31 && scanBytes <= 31
            else -> advBytes <= 31
        }
    }

    /**
     * 获取广播数据的字节长度
     */
    fun getAdvDataByteLength(): Int {
        return advDataHex.length / 2
    }

    /**
     * 获取扫描响应数据的字节长度
     */
    fun getScanResponseByteLength(): Int {
        return scanResponseHex.length / 2
    }

    /**
     * 获取总数据字节长度
     */
    fun getTotalDataByteLength(): Int {
        return getAdvDataByteLength() + getScanResponseByteLength()
    }

    /**
     * 获取当前使用的广播模式
     */
    fun getAdvertisingMode(): AdvertisingMode {
        return when {
            useExtendedAdvertising -> AdvertisingMode.EXTENDED
            getScanResponseByteLength() > 0 -> AdvertisingMode.LEGACY_WITH_SCAN_RESPONSE
            else -> AdvertisingMode.LEGACY
        }
    }

    /**
     * 自动分割广播数据
     * 当advDataHex长度超过31字节时，自动分割为adv + scan response
     * @return Pair(advDataHex, scanResponseHex)
     */
    fun autoSplitAdvData(): Pair<String, String> {
        val totalBytes = getAdvDataByteLength()

        return when {
            totalBytes <= 31 -> Pair(advDataHex, "")
            totalBytes <= 62 -> {
                // 分割为31字节广播 + 剩余扫描响应
                val advPart = advDataHex.substring(0, 62)
                val scanPart = advDataHex.substring(62)
                Pair(advPart, scanPart)
            }
            else -> {
                // 超过62字节，使用扩展广播模式
                Pair(advDataHex, "")
            }
        }
    }
}