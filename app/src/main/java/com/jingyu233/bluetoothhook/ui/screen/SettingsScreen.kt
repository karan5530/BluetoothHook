package com.jingyu233.bluetoothhook.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingyu233.bluetoothhook.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val hookStatus by viewModel.hookStatus.collectAsState()
    val deviceCount by viewModel.deviceCount.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val connectionTestResult by viewModel.connectionTestResult.collectAsState()
    val importExportStatus by viewModel.importExportStatus.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()

    // 文件选择器：导入 JSON
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.importDevices(uri, replaceExisting = false)
    }

    // 文件选择器：导出 JSON
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        viewModel.exportDevices(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hook状态部分
            item {
                SettingsSection(title = "Hook状态") {
                    HookStatusDetailCard(
                        status = hookStatus,
                        deviceCount = deviceCount,
                        onRefresh = { viewModel.refreshHookStatus() }
                    )
                }
            }

            // WebDAV同步部分
            item {
                SettingsSection(title = "WebDAV 同步") {
                    WebDavSettingsCard(
                        settings = settings,
                        isTestingConnection = isTestingConnection,
                        connectionTestResult = connectionTestResult,
                        syncLogs = syncLogs,
                        onUrlChange = { viewModel.updateWebDavUrl(it) },
                        onUsernameChange = { viewModel.updateWebDavUsername(it) },
                        onPasswordChange = { viewModel.updateWebDavPassword(it) },
                        onAutoSyncToggle = { viewModel.toggleAutoSync(it) },
                        onSyncIntervalChange = { viewModel.updateSyncInterval(it) },
                        onTestConnection = { viewModel.testWebDavConnection() },
                        onSyncNow = { viewModel.syncNow() },
                        onClearTestResult = { viewModel.clearConnectionTestResult() }
                    )
                }
            }

            // 导入/导出部分
            item {
                SettingsSection(title = "导入/导出") {
                    ImportExportCard(
                        onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        onExport = { exportLauncher.launch("bluetooth_hook_devices.json") },
                        onClearData = { viewModel.clearAllData() },
                        status = importExportStatus,
                        onClearStatus = { viewModel.clearImportExportStatus() }
                    )
                }
            }

            // 关于部分
            item {
                SettingsSection(title = "关于") {
                    AboutCard()
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun HookStatusDetailCard(
    status: String,
    deviceCount: Int,
    onRefresh: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "当前状态: $status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已配置 $deviceCount 个虚拟设备",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "使用说明",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. 在LSPosed管理器中启用本模块\n" +
                        "2. 确保模块作用域包含\"系统框架\"和\"com.android.bluetooth\"\n" +
                        "3. 重启系统或重启蓝牙服务\n" +
                        "4. Hook激活后，虚拟设备会自动出现在蓝牙扫描结果中",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WebDavSettingsCard(
    settings: com.jingyu233.bluetoothhook.data.model.AppSettings,
    isTestingConnection: Boolean,
    connectionTestResult: String?,
    syncLogs: List<com.jingyu233.bluetoothhook.data.model.SyncLog>,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAutoSyncToggle: (Boolean) -> Unit,
    onSyncIntervalChange: (Int) -> Unit,
    onTestConnection: () -> Unit,
    onSyncNow: () -> Unit,
    onClearTestResult: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "配置WebDAV服务器以跨设备同步虚拟设备配置",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = settings.webdavUrl,
                onValueChange = onUrlChange,
                label = { Text("WebDAV URL") },
                placeholder = { Text("https://example.com/dav") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                leadingIcon = { Icon(Icons.Default.AccountBox, null) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = settings.webdavUsername,
                onValueChange = onUsernameChange,
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = settings.webdavPassword,
                onValueChange = onPasswordChange,
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Warning else Icons.Default.Lock,
                            if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自动同步")
                Switch(
                    checked = settings.autoSyncEnabled,
                    onCheckedChange = onAutoSyncToggle
                )
            }

            // 同步间隔输入框 (只在启用自动同步时显示)
            if (settings.autoSyncEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = settings.syncIntervalSeconds.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { seconds ->
                            if (seconds > 0) {
                                onSyncIntervalChange(seconds)
                            }
                        }
                    },
                    label = { Text("同步间隔(秒)") },
                    placeholder = { Text("60") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Refresh, null) },
                    supportingText = { Text("建议设置10秒以上") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f),
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("测试连接")
                    }
                }

                Button(
                    onClick = onSyncNow,
                    modifier = Modifier.weight(1f),
                    enabled = !isTestingConnection
                ) {
                    Text("立即同步")
                }
            }

            // 测试结果
            connectionTestResult?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearTestResult) {
                            Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 同步日志
            if (syncLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "同步日志 (最近3次)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                syncLogs.forEach { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (log.success)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                        .format(java.util.Date(log.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (log.details.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportCard(
    onImport: () -> Unit,
    onExport: () -> Unit,
    onClearData: () -> Unit,
    status: String?,
    onClearStatus: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "导入和导出JSON配置文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导入")
                }

                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导出")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 清理数据按钮
            OutlinedButton(
                onClick = onClearData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("清理所有数据")
            }

            // 显示导入导出状态
            if (status != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            status.contains("成功") -> MaterialTheme.colorScheme.primaryContainer
                            status.contains("失败") -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearStatus,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "• 导出：将当前所有设备配置保存为JSON文件\n• 导入：从JSON文件加载设备配置（追加模式）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AboutCard() {
    val context = LocalContext.current
    val githubUrl = "https://github.com/jingyu233/bluetoothhook"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BluetoothHook",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "版本 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "功能特性",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• 向系统蓝牙扫描结果注入虚拟BLE设备\n" +
                        "• 自定义MAC地址、RSSI和广播数据\n" +
                        "• 支持动态广播数据（传感器模拟）\n" +
                        "• WebDAV配置同步\n" +
                        "• 导入/导出JSON配置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "用途",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本模块设计用于蓝牙应用开发和调试，避免反复连接真实设备的麻烦。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // GitHub链接和邀请Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "开源项目",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = githubUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                            context.startActivity(intent)
                        }
                    )
                }

                // Star按钮
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Star", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // 免责声明
            Text(
                text = "⚠️ 免责声明",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "本软件仅供学习研究和合法测试使用。严禁用于以下用途：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 非法入侵、破坏他人设备或系统\n" +
                                "• 窃取、篡改他人数据或隐私信息\n" +
                                "• 制造虚假信息或欺诈行为\n" +
                                "• 干扰正常通信和服务\n" +
                                "• 任何违反法律法规的行为",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "使用本软件即表示您同意遵守相关法律法规，并对自己的行为负全部责任。开发者不对任何滥用行为承担责任。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}
