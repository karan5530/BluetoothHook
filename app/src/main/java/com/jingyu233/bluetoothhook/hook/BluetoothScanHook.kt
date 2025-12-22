package com.jingyu233.bluetoothhook.hook

import com.jingyu233.bluetoothhook.utils.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * 蓝牙扫描Hook核心类
 * 负责拦截ScanController.onScanResultInternal并注入虚拟设备
 */
class BluetoothScanHook(
    private val classLoader: ClassLoader,
    private val prefs: XSharedPreferences
) {
    companion object {
        private val TAG = Logger.Tags.HOOK_SCANNER
        private const val CLASS_SCAN_CONTROLLER = "com.android.bluetooth.le_scan.ScanController"
        private const val METHOD_ON_SCAN_RESULT_INTERNAL = "onScanResultInternal"
    }

    private lateinit var scanResultBuilder: ScanResultBuilder
    private lateinit var virtualDeviceInjector: VirtualDeviceInjector

    fun init() {
        try {
            Logger.Hook.i(TAG, "Initializing BluetoothScanHook")

            // 初始化辅助工具
            scanResultBuilder = ScanResultBuilder(classLoader)
            virtualDeviceInjector = VirtualDeviceInjector(scanResultBuilder, prefs)

            // Hook主要方法
            val hooked = hookScanResultInternal()

            if (hooked) {
                Logger.Hook.i(TAG, "Successfully hooked ScanController.onScanResultInternal")
            } else {
                Logger.Hook.e(TAG, "Failed to hook scan methods", null)
            }

        } catch (e: Throwable) {
            Logger.Hook.e(TAG, "Failed to initialize BluetoothScanHook", e)
        }
    }

    /**
     * Hook ScanController.onScanResultInternal方法
     * 这是逆向代码分析确定的最佳注入点（line 362）
     */
    private fun hookScanResultInternal(): Boolean {
        return try {
            val scanControllerClass = XposedHelpers.findClass(CLASS_SCAN_CONTROLLER, classLoader)

            // 查找方法（根据逆向代码分析的签名）
            // onScanResultInternal(int eventType, int addressType, String address,
            //                      int primaryPhy, int secondaryPhy, int advertisingSid,
            //                      int txPower, int rssi, int periodicAdvInt,
            //                      byte[] scanRecord, String originalAddress)
            val method = XposedHelpers.findMethodBestMatch(
                scanControllerClass,
                METHOD_ON_SCAN_RESULT_INTERNAL,
                Int::class.javaPrimitiveType,    // eventType
                Int::class.javaPrimitiveType,    // addressType
                String::class.java,              // address
                Int::class.javaPrimitiveType,    // primaryPhy
                Int::class.javaPrimitiveType,    // secondaryPhy
                Int::class.javaPrimitiveType,    // advertisingSid
                Int::class.javaPrimitiveType,    // txPower
                Int::class.javaPrimitiveType,    // rssi
                Int::class.javaPrimitiveType,    // periodicAdvInt
                ByteArray::class.java,           // scanRecord
                String::class.java               // originalAddress
            )

            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        // 在原方法执行完后注入虚拟设备
                        injectVirtualDevices(param)
                    } catch (e: Throwable) {
                        // 捕获所有异常，避免影响真实扫描结果
                        Logger.Hook.e(TAG, "Error during virtual device injection", e)
                    }
                }
            })

            true
        } catch (e: Throwable) {
            Logger.Hook.e(TAG, "Failed to hook $METHOD_ON_SCAN_RESULT_INTERNAL", e)
            false
        }
    }

    /**
     * 注入虚拟设备到扫描结果
     * 在真实扫描结果处理完后调用
     */
    private fun injectVirtualDevices(param: XC_MethodHook.MethodHookParam) {
        try {
            // 重新加载配置（SharedPreferences可能被UI进程更新）
            prefs.reload()

            // 检查全局开关
            val globalEnabled = prefs.getBoolean("global_enabled", true)
            if (!globalEnabled) {
                return // 全局开关关闭，静默返回
            }

            // 获取ScanController实例
            val scanControllerInstance = param.thisObject

            // 获取mScanManager字段（根据逆向代码：line 410）
            val scanManager = XposedHelpers.getObjectField(scanControllerInstance, "mScanManager")
            if (scanManager == null) {
                return
            }

            // 获取mScannerMap字段
            val scannerMap = XposedHelpers.getObjectField(scanControllerInstance, "mScannerMap")
            if (scannerMap == null) {
                return
            }

            // 获取当前扫描队列（line 410: mScanManager.getRegularScanQueue()）
            val scanQueue = XposedHelpers.callMethod(scanManager, "getRegularScanQueue") as? Collection<*>
            if (scanQueue == null || scanQueue.isEmpty()) {
                return // 没有活跃的扫描客户端，静默返回
            }

            // 执行虚拟设备注入
            virtualDeviceInjector.injectDevices(
                scanControllerInstance,
                scanManager,
                scannerMap,
                scanQueue
            )

        } catch (e: Throwable) {
            Logger.Hook.e(TAG, "Error in injectVirtualDevices", e)
        }
    }
}
