package com.jingyu233.bluetoothhook.sync

import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import com.jingyu233.bluetoothhook.utils.Logger
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.ByteArrayInputStream

/**
 * WebDAV 客户端
 * 负责与 WebDAV 服务器通信，上传/下载设备配置
 */
class WebDavClient(
    private val url: String,
    private val username: String,
    private val password: String
) {

    companion object {
        private val TAG = Logger.Tags.DATA_REPOSITORY
        private const val REMOTE_FILE_NAME = "bluetooth_hook_devices.json"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 测试 WebDAV 连接
     * @return Result.success(true) 如果连接成功
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val sardine = OkHttpSardine()
            sardine.setCredentials(username, password)

            // 尝试列出根目录
            val resources = sardine.list(url)

            Logger.App.i(TAG, "WebDAV connection test successful: ${resources.size} resources found")
            Result.success(true)

        } catch (e: Exception) {
            Logger.App.e(TAG, "WebDAV connection test failed", e)
            Result.failure(e)
        }
    }

    /**
     * 上传设备列表到 WebDAV 服务器
     */
    suspend fun uploadDevices(devices: List<VirtualDevice>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sardine = OkHttpSardine()
            sardine.setCredentials(username, password)

            // 构建载荷
            val payload = DevicesPayload(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceCount = devices.size,
                devices = devices
            )

            // 序列化为 JSON
            val jsonString = json.encodeToString(payload)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            // 计算完整的文件 URL
            val fileUrl = buildFileUrl(url, REMOTE_FILE_NAME)

            // 上传文件（Sardine put 方法签名：put(url, data, contentType)）
            sardine.put(fileUrl, jsonBytes, "application/json")

            Logger.App.i(TAG, "Uploaded ${devices.size} devices to WebDAV: $fileUrl")
            Result.success(Unit)

        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to upload devices to WebDAV", e)
            Result.failure(e)
        }
    }

    /**
     * 从 WebDAV 服务器下载设备列表
     */
    suspend fun downloadDevices(): Result<List<VirtualDevice>> = withContext(Dispatchers.IO) {
        try {
            val sardine = OkHttpSardine()
            sardine.setCredentials(username, password)

            // 计算完整的文件 URL
            val fileUrl = buildFileUrl(url, REMOTE_FILE_NAME)

            // 检查文件是否存在
            if (!sardine.exists(fileUrl)) {
                Logger.App.w(TAG, "Remote file does not exist: $fileUrl")
                return@withContext Result.success(emptyList())
            }

            // 下载文件
            val inputStream = sardine.get(fileUrl)
            val jsonString = inputStream.readBytes().toString(Charsets.UTF_8)

            // 解析 JSON
            val payload = json.decodeFromString<DevicesPayload>(jsonString)

            Logger.App.i(TAG, "Downloaded ${payload.devices.size} devices from WebDAV")
            Result.success(payload.devices)

        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to download devices from WebDAV", e)
            Result.failure(e)
        }
    }

    /**
     * 获取远程文件的时间戳
     */
    suspend fun getRemoteTimestamp(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val sardine = OkHttpSardine()
            sardine.setCredentials(username, password)

            val fileUrl = buildFileUrl(url, REMOTE_FILE_NAME)

            if (!sardine.exists(fileUrl)) {
                return@withContext Result.success(0L)
            }

            // 下载并解析文件获取时间戳
            val inputStream = sardine.get(fileUrl)
            val jsonString = inputStream.readBytes().toString(Charsets.UTF_8)
            val payload = json.decodeFromString<DevicesPayload>(jsonString)

            Result.success(payload.timestamp)

        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to get remote timestamp", e)
            Result.failure(e)
        }
    }

    /**
     * 构建文件完整 URL
     * 处理 URL 末尾斜杠问题
     */
    private fun buildFileUrl(baseUrl: String, fileName: String): String {
        val cleanUrl = baseUrl.trimEnd('/')
        return "$cleanUrl/$fileName"
    }
}

/**
 * WebDAV 存储的数据载荷
 */
@Serializable
data class DevicesPayload(
    val version: Int,
    val timestamp: Long,
    val deviceCount: Int,
    val devices: List<VirtualDevice>
)
