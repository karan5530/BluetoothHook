package com.jingyu233.bluetoothhook.utils

import android.util.Log
import de.robv.android.xposed.XposedBridge

/**
 * 统一的日志工具类
 * 使用层级化标签格式: BTHook:Layer:Component
 *
 * 使用方法:
 * - Hook层: Logger.Hook.d(TAG, message)
 * - App层: Logger.App.d(TAG, message)
 */
object Logger {

    /**
     * Hook层日志（运行在系统蓝牙进程）
     * 使用 XposedBridge.log()
     */
    object Hook {
        fun v(tag: String, message: String) {
            XposedBridge.log("[$tag] VERBOSE: $message")
        }

        fun d(tag: String, message: String) {
            XposedBridge.log("[$tag] DEBUG: $message")
        }

        fun i(tag: String, message: String) {
            XposedBridge.log("[$tag] $message")
        }

        fun w(tag: String, message: String) {
            XposedBridge.log("[$tag] WARN: $message")
        }

        fun e(tag: String, message: String, throwable: Throwable? = null) {
            XposedBridge.log("[$tag] ERROR: $message")
            throwable?.let { XposedBridge.log(it) }
        }
    }

    /**
     * App层日志（运行在模块UI进程）
     * 使用 Android Log.*
     */
    object App {
        fun v(tag: String, message: String) {
            Log.v(tag, message)
        }

        fun d(tag: String, message: String) {
            Log.d(tag, message)
        }

        fun i(tag: String, message: String) {
            Log.i(tag, message)
        }

        fun w(tag: String, message: String) {
            Log.w(tag, message)
        }

        fun e(tag: String, message: String, throwable: Throwable? = null) {
            Log.e(tag, message, throwable)
        }
    }

    /**
     * 预定义的标签常量
     */
    object Tags {
        // Core
        const val CORE_ENTRY = "BTHook:Core:Entry"

        // Hook层
        const val HOOK_SCANNER = "BTHook:Hook:Scanner"
        const val HOOK_INJECTOR = "BTHook:Hook:Injector"
        const val HOOK_BUILDER = "BTHook:Hook:Builder"

        // Engine
        const val ENGINE_DYNAMIC = "BTHook:Engine:DynamicData"

        // Data层
        const val DATA_BRIDGE = "BTHook:Data:Bridge"
        const val DATA_REPOSITORY = "BTHook:Data:Repository"
        const val DATA_DATABASE = "BTHook:Data:Database"

        // UI ViewModel
        const val UI_VM_DEVICE_LIST = "BTHook:UI:VM:DeviceList"
        const val UI_VM_DEVICE_EDITOR = "BTHook:UI:VM:DeviceEditor"
        const val UI_VM_SETTINGS = "BTHook:UI:VM:Settings"

        // Sync
        const val SYNC = "BTHook:Sync"

        // UI Screen
        const val UI_SCREEN_DEVICE_LIST = "BTHook:UI:Screen:DeviceList"
        const val UI_SCREEN_DEVICE_EDITOR = "BTHook:UI:Screen:DeviceEditor"
        const val UI_SCREEN_SETTINGS = "BTHook:UI:Screen:Settings"
    }
}
