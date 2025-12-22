package com.jingyu233.bluetoothhook.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import com.jingyu233.bluetoothhook.ui.viewmodel.DeviceListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel = viewModel(),
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val devices by viewModel.devices.collectAsState(initial = emptyList())
    val globalEnabled by viewModel.globalEnabled.collectAsState()
    val hookStatus by viewModel.hookStatus.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<VirtualDevice?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("虚拟蓝牙设备") },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshHookStatus()
                    }) {
                        Icon(Icons.Default.Refresh, "刷新Hook状态")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, "添加设备")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hook状态卡片
            HookStatusCard(
                status = hookStatus,
                modifier = Modifier.padding(16.dp)
            )

            // 全局开关
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用虚拟设备",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "主开关，关闭后所有虚拟设备都不会注入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = globalEnabled,
                        onCheckedChange = { viewModel.setGlobalEnabled(it) }
                    )
                }
            }

            // 设备列表
            if (devices.isEmpty()) {
                // 空状态
                EmptyState(
                    onAddDevice = { onNavigateToEditor(null) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(devices, key = { it.id }) { device ->
                        DeviceListItem(
                            device = device,
                            onToggle = { viewModel.toggleDevice(device) },
                            onEdit = { onNavigateToEditor(device.id) },
                            onDelete = { showDeleteDialog = device },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { device ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("删除设备") },
            text = { Text("确定要删除设备 \"${device.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDevice(device)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun HookStatusCard(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (status) {
        "Active" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Default.CheckCircle,
            "Hook已激活"
        )
        "Unknown" -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            Icons.Default.Info,
            "Hook状态未知"
        )
        else -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Default.Warning,
            "Hook未激活"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "请在LSPosed中启用模块并重启蓝牙服务",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DeviceListItem(
    device: VirtualDevice,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (device.enabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = device.mac,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(
                    checked = device.enabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RSSI: ${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "间隔: ${device.intervalMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${device.getAdvDataByteLength()} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有虚拟设备",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右下角的 + 按钮添加你的第一个虚拟设备",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddDevice) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加虚拟设备")
        }
    }
}
