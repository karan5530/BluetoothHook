package com.jingyu233.bluetoothhook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jingyu233.bluetoothhook.ui.screen.DeviceEditorScreen
import com.jingyu233.bluetoothhook.ui.screen.DeviceListScreen
import com.jingyu233.bluetoothhook.ui.screen.SettingsScreen

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    object DeviceList : Screen("device_list")
    object DeviceEditor : Screen("device_editor/{deviceId}") {
        fun createRoute(deviceId: String?) =
            if (deviceId != null) "device_editor/$deviceId" else "device_editor/new"
    }
    object Settings : Screen("settings")
}

/**
 * 应用导航
 */
@Composable
fun BluetoothHookNavigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Screen.DeviceList.route) {
        // 设备列表屏幕
        composable(Screen.DeviceList.route) {
            DeviceListScreen(
                onNavigateToEditor = { deviceId ->
                    navController.navigate(Screen.DeviceEditor.createRoute(deviceId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 设备编辑器屏幕
        composable(
            route = Screen.DeviceEditor.route,
            arguments = listOf(navArgument("deviceId") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")
            DeviceEditorScreen(
                deviceId = if (deviceId == "new") null else deviceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 设置屏幕
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
