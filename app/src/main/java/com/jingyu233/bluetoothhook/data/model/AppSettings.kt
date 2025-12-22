package com.jingyu233.bluetoothhook.data.model

import kotlinx.serialization.Serializable

/**
 * 应用设置数据模型
 */
@Serializable
data class AppSettings(
    val globalEnabled: Boolean = true,
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPassword: String = "",
    val autoSyncEnabled: Boolean = false,
    val syncIntervalSeconds: Int = 60,  // 同步间隔(秒)
    val logLevel: LogLevel = LogLevel.INFO
)

/**
 * 日志级别
 */
@Serializable
enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

/**
 * Hook状态
 */
enum class HookStatus {
    UNKNOWN,    // 未知状态
    ACTIVE,     // Hook激活
    INACTIVE,   // Hook未激活
    ERROR       // Hook出错
}
