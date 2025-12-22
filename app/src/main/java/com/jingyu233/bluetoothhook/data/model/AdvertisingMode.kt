package com.jingyu233.bluetoothhook.data.model

/**
 * BLE广播模式
 */
enum class AdvertisingMode {
    /**
     * 传统广播模式（Legacy Advertising）
     * - 最多31字节广播数据
     * - 兼容所有BLE设备
     */
    LEGACY,

    /**
     * 传统广播 + 扫描响应模式
     * - 最多31字节广播数据 + 31字节扫描响应
     * - 总共最多62字节
     * - 兼容所有BLE设备
     */
    LEGACY_WITH_SCAN_RESPONSE,

    /**
     * 扩展广播模式（Extended Advertising）
     * - 最多254字节广播数据
     * - 需要BLE 5.0+硬件支持
     */
    EXTENDED
}
