package com.jingyu233.bluetoothhook.data.bridge

import android.content.Context
import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * 配置桥接器
 * 负责在UI进程和Hook进程之间同步配置数据
 * 使用LSPosed的New XSharedPreferences特性（MODE_WORLD_READABLE）
 */
class ConfigBridge(private val context: Context) {

    companion object {
        private val TAG = Logger.Tags.DATA_BRIDGE

        // SharedPreferences文件名（Hook进程将通过XSharedPreferences访问）
        const val PREF_NAME = "hook_config"

        // SharedPreferences键名
        private const val KEY_DEVICES = "devices"
        private const val KEY_GLOBAL_ENABLED = "global_enabled"
        private const val KEY_LAST_UPDATED = "last_updated"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * 从UI进程写入设备配置
     * 将Room数据库的设备列表同步到SharedPreferences
     * 使用MODE_WORLD_READABLE允许Hook进程通过XSharedPreferences读取
     */
    @Suppress("DEPRECATION")
    fun writeDeviceConfig(devices: List<VirtualDevice>) {
        try {
            // 使用MODE_WORLD_READABLE允许跨进程访问（LSPosed支持）
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE)

            // 序列化设备列表为JSON
            val devicesJson = json.encodeToString(devices)

            // 使用commit()而不是apply()确保立即写入
            prefs.edit()
                .putString(KEY_DEVICES, devicesJson)
                .putBoolean(KEY_GLOBAL_ENABLED, getGlobalEnabled())
                .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                .commit() // 立即同步写入

            Logger.App.d(TAG, "Wrote ${devices.size} devices to SharedPreferences (MODE_WORLD_READABLE)")

        } catch (e: SecurityException) {
            Logger.App.e(TAG, "MODE_WORLD_READABLE not supported - LSPosed not enabled?", e)
        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to write device config", e)
        }
    }

    /**
     * 设置全局开关状态
     */
    @Suppress("DEPRECATION")
    fun setGlobalEnabled(enabled: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE)
            prefs.edit()
                .putBoolean(KEY_GLOBAL_ENABLED, enabled)
                .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                .commit()

            Logger.App.d(TAG, "Set global_enabled = $enabled")

        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to set global enabled state", e)
        }
    }

    /**
     * 获取全局开关状态
     */
    @Suppress("DEPRECATION")
    fun getGlobalEnabled(): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE)
            prefs.getBoolean(KEY_GLOBAL_ENABLED, true)
        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to get global enabled state", e)
            true // 默认启用
        }
    }

    /**
     * 获取配置最后更新时间
     */
    @Suppress("DEPRECATION")
    fun getLastUpdatedTime(): Long {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE)
            prefs.getLong(KEY_LAST_UPDATED, 0L)
        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to get last updated time", e)
            0L
        }
    }

    /**
     * 设置Hook状态（由Hook进程写入，仅用于UI显示）
     * 注意：这个方法可能需要使用不同的SharedPreferences文件
     */
    fun setHookStatus(status: String) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("hook_status", status)
            .putLong("hook_status_updated", System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取Hook状态（UI进程读取）
     * 从module_status SharedPreferences读取激活标记
     */
    fun getHookStatus(): String {
        return try {
            val prefs = context.getSharedPreferences("module_status", Context.MODE_PRIVATE)
            val isActive = prefs.getBoolean("xposed_active", false)
            val lastHookTime = prefs.getLong("last_hook_time", 0L)

            if (isActive) {
                // 检查最后Hook时间是否在合理范围内(5分钟)
                val timeDiff = System.currentTimeMillis() - lastHookTime
                if (timeDiff < 5 * 60 * 1000) {
                    "Active"
                } else {
                    "Inactive"
                }
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to read hook status", e)
            "Unknown"
        }
    }

    /**
     * 清空所有配置
     */
    @Suppress("DEPRECATION")
    fun clearAll() {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE)
            prefs.edit().clear().commit()

            val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appPrefs.edit().clear().apply()

            Logger.App.w(TAG, "Cleared all configuration")
        } catch (e: Exception) {
            Logger.App.e(TAG, "Failed to clear config", e)
        }
    }
}
