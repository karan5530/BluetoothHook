package com.jingyu233.bluetoothhook.hook

import com.jingyu233.bluetoothhook.utils.Logger
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Xposed模块入口点
 * 负责初始化Hook并拦截com.android.bluetooth进程
 */
class HookEntry : IXposedHookLoadPackage {

    companion object {
        private val TAG = Logger.Tags.CORE_ENTRY
        private const val PACKAGE_BLUETOOTH = "com.android.bluetooth"
        private const val PREF_PACKAGE = "com.jingyu233.bluetoothhook"
        private const val PREF_NAME = "hook_config" // Must match ConfigBridge.PREF_NAME
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook自身Application以检测模块激活状态
        if (lpparam.packageName == PREF_PACKAGE) {
            hookModuleApplication(lpparam)
            return
        }

        // 只Hook蓝牙系统进程
        if (lpparam.packageName != PACKAGE_BLUETOOTH) {
            return
        }

        try {
            Logger.Hook.i(TAG, "Initializing BluetoothHook in ${lpparam.packageName}")

            // 初始化SharedPreferences以读取模块配置
            // 使用XSharedPreferences可以跨进程读取（系统进程读取模块进程的配置）
            val prefs = XSharedPreferences(PREF_PACKAGE, PREF_NAME)

            // 设置文件权限为world-readable（在模块进程中设置，这里只是尝试）
            try {
                prefs.makeWorldReadable()
            } catch (e: Exception) {
                Logger.Hook.w(TAG, "Failed to make preferences world-readable: ${e.message}")
            }

            // 检查模块是否启用
            prefs.reload()
            val enabled = prefs.getBoolean("global_enabled", true)

            if (!enabled) {
                Logger.Hook.i(TAG, "Module is disabled by user")
                return
            }

            // 初始化蓝牙扫描Hook
            val bluetoothScanHook = BluetoothScanHook(lpparam.classLoader, prefs)
            bluetoothScanHook.init()

            // 写入Hook状态标记（供UI进程读取）
            writeHookStatus()

            Logger.Hook.i(TAG, "BluetoothHook initialized successfully")

        } catch (e: Throwable) {
            // 捕获所有异常，防止蓝牙服务崩溃
            Logger.Hook.e(TAG, "Fatal error during initialization", e)
        }
    }

    /**
     * Hook模块自身的Application.onCreate
     * 写入激活标记供UI读取
     */
    private fun hookModuleApplication(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                lpparam.classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val context = param.thisObject as android.content.Context
                            val prefs = context.getSharedPreferences(
                                "module_status",
                                android.content.Context.MODE_PRIVATE
                            )
                            prefs.edit()
                                .putBoolean("xposed_active", true)
                                .putLong("last_hook_time", System.currentTimeMillis())
                                .apply()

                            Logger.Hook.i(TAG, "Module activation marker written successfully")
                        } catch (e: Exception) {
                            Logger.Hook.e(TAG, "Failed to write activation marker", e)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Logger.Hook.e(TAG, "Failed to hook module application", e)
        }
    }

    /**
     * 写入Hook状态到文件系统
     * Hook进程运行在系统进程中,使用 /data/system 目录以避免 SELinux 权限问题
     */
    private fun writeHookStatus() {
        try {
            // 方案1: 尝试写入系统可访问的目录
            val statusFile = java.io.File("/data/system/bluetooth_hook_status.txt")

            // 写入状态和时间戳
            val statusData = "Active|${System.currentTimeMillis()}"
            statusFile.writeText(statusData)

            // 设置文件权限为全局可读（0644）
            try {
                Runtime.getRuntime().exec("chmod 644 ${statusFile.absolutePath}").waitFor()
            } catch (e: Exception) {
                Logger.Hook.w(TAG, "Failed to set file permissions: ${e.message}")
            }

            Logger.Hook.d(TAG, "Hook status written to /data/system: $statusData")
        } catch (e: Exception) {
            Logger.Hook.e(TAG, "Failed to write hook status file", e)
        }
    }
}
