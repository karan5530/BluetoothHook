package com.jingyu233.bluetoothhook.data.model

import kotlinx.serialization.Serializable

/**
 * 同步日志记录
 */
@Serializable
data class SyncLog(
    val timestamp: Long,
    val success: Boolean,
    val message: String,
    val details: String = ""
)
