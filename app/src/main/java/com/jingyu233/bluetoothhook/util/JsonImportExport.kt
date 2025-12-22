package com.jingyu233.bluetoothhook.util

import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * JSON 导入导出工具类
 * 负责设备列表的序列化和反序列化
 */
object JsonImportExport {

    private val TAG = Logger.Tags.DATA_REPOSITORY

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 导出设备列表到 JSON 字符串
     */
    fun exportToJson(devices: List<VirtualDevice>): String {
        val payload = DevicesPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            deviceCount = devices.size,
            devices = devices
        )

        return json.encodeToString(payload)
    }

    /**
     * 从 JSON 字符串导入设备列表
     * @return Result.success(设备列表) 或 Result.failure(异常)
     */
    fun importFromJson(jsonString: String): Result<List<VirtualDevice>> {
        return try {
            // 验证 JSON 结构
            validateJsonStructure(jsonString).getOrThrow()

            // 解析 JSON
            val payload = json.decodeFromString<DevicesPayload>(jsonString)

            // 验证版本
            if (payload.version > 1) {
                return Result.failure(
                    IllegalArgumentException("不支持的配置文件版本: ${payload.version}，请更新应用")
                )
            }

            // 验证设备数量
            if (payload.devices.size != payload.deviceCount) {
                Logger.App.w(TAG, "设备数量不匹配: 声明=${payload.deviceCount}, 实际=${payload.devices.size}")
            }

            // 验证每个设备的有效性
            val validDevices = payload.devices.filter { device ->
                if (!device.isValid()) {
                    Logger.App.w(TAG, "跳过无效设备: ${device.name}")
                    false
                } else {
                    true
                }
            }

            Logger.App.i(TAG, "成功导入 ${validDevices.size}/${payload.devices.size} 个设备")
            Result.success(validDevices)

        } catch (e: Exception) {
            Logger.App.e(TAG, "JSON 导入失败", e)
            Result.failure(e)
        }
    }

    /**
     * 验证 JSON 字符串格式
     */
    fun validateJsonStructure(jsonString: String): Result<Unit> {
        return try {
            if (jsonString.isBlank()) {
                return Result.failure(IllegalArgumentException("JSON 内容为空"))
            }

            // 尝试解析为 JSON 对象
            json.decodeFromString<DevicesPayload>(jsonString)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("无效的 JSON 格式: ${e.message}", e))
        }
    }
}

/**
 * 导入导出的数据载荷
 */
@Serializable
data class DevicesPayload(
    val version: Int,                   // 配置文件版本
    val exportedAt: Long,               // 导出时间戳
    val deviceCount: Int,               // 设备数量
    val devices: List<VirtualDevice>    // 设备列表
)
