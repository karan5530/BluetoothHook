package com.jingyu233.bluetoothhook.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingyu233.bluetoothhook.data.model.AdvertisingMode
import com.jingyu233.bluetoothhook.ui.viewmodel.DeviceEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditorScreen(
    deviceId: String?,
    onNavigateBack: () -> Unit,
    viewModel: DeviceEditorViewModel = viewModel()
) {
    val device by viewModel.device.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (deviceId == null) "新建虚拟设备" else "编辑虚拟设备") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveDevice {
                                onNavigateBack()
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, "保存")
                        }
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
            // 设备名称
            item {
                OutlinedTextField(
                    value = device.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("设备名称") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = "name" in validationErrors,
                    supportingText = {
                        validationErrors["name"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true
                )
            }

            // MAC地址
            item {
                OutlinedTextField(
                    value = device.mac,
                    onValueChange = { viewModel.updateMac(it) },
                    label = { Text("MAC地址") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = "mac" in validationErrors,
                    supportingText = {
                        validationErrors["mac"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        } ?: Text("格式: AA:BB:CC:DD:EE:FF")
                    },
                    singleLine = true,
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") }
                )
            }

            // RSSI滑块
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "信号强度 (RSSI)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("-100 dBm", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${device.rssi} dBm",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("0 dBm", style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = device.rssi.toFloat(),
                            onValueChange = { viewModel.updateRssi(it.toInt()) },
                            valueRange = -100f..0f,
                            steps = 99
                        )
                        validationErrors["rssi"]?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 广播数据
            item {
                val mode = device.getAdvertisingMode()
                val maxBytes = when (mode) {
                    AdvertisingMode.LEGACY -> 31
                    AdvertisingMode.LEGACY_WITH_SCAN_RESPONSE -> 62
                    AdvertisingMode.EXTENDED -> 254
                }
                val modeText = when (mode) {
                    AdvertisingMode.LEGACY -> "传统广播"
                    AdvertisingMode.LEGACY_WITH_SCAN_RESPONSE -> "传统+扫描响应"
                    AdvertisingMode.EXTENDED -> "扩展广播"
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = device.advDataHex,
                        onValueChange = { viewModel.updateAdvData(it) },
                        label = { Text("广播数据 (十六进制)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = "advData" in validationErrors,
                        supportingText = {
                            validationErrors["advData"]?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            } ?: Text("${device.getAdvDataByteLength()} / ${if (device.useExtendedAdvertising) 254 else 31} bytes")
                        },
                        minLines = 3,
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )

                    // 扫描响应数据输入框（仅在非扩展广播模式下显示）
                    if (!device.useExtendedAdvertising) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = device.scanResponseHex,
                            onValueChange = { viewModel.updateScanResponse(it) },
                            label = { Text("扫描响应数据 (十六进制, 可选)") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Text("${device.getScanResponseByteLength()} / 31 bytes")
                            },
                            minLines = 2,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    // 显示模式信息卡片
                    if (device.getTotalDataByteLength() > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (mode) {
                                    AdvertisingMode.LEGACY -> MaterialTheme.colorScheme.primaryContainer
                                    AdvertisingMode.LEGACY_WITH_SCAN_RESPONSE -> MaterialTheme.colorScheme.secondaryContainer
                                    AdvertisingMode.EXTENDED -> MaterialTheme.colorScheme.tertiaryContainer
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "广播模式: $modeText",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                when (mode) {
                                    AdvertisingMode.LEGACY -> {
                                        Text(
                                            "广播数据: ${device.getAdvDataByteLength()} bytes",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    AdvertisingMode.LEGACY_WITH_SCAN_RESPONSE -> {
                                        Text(
                                            "广播: ${device.getAdvDataByteLength()} bytes + 扫描响应: ${device.getScanResponseByteLength()} bytes",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    AdvertisingMode.EXTENDED -> {
                                        Text(
                                            "扩展广播: ${device.getAdvDataByteLength()} bytes (需要BLE 5.0+)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 广播模式切换按钮
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 自动分割按钮 (当数据 > 31 bytes且未使用扩展广播时)
                        if (device.getAdvDataByteLength() > 31 && !device.useExtendedAdvertising && device.getScanResponseByteLength() == 0) {
                            OutlinedButton(
                                onClick = { viewModel.autoSplitAdvData() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("自动分割")
                            }
                        }

                        // 切换到扩展广播模式按钮
                        if (device.getAdvDataByteLength() > 31) {
                            OutlinedButton(
                                onClick = { viewModel.toggleExtendedAdvertising() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (device.useExtendedAdvertising) "切换到传统模式" else "使用扩展广播")
                            }
                        }
                    }
                }
            }

            // 自动生成广播数据按钮
            item {
                OutlinedButton(
                    onClick = { viewModel.generateStandardAdvData() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = device.name.isNotBlank()
                ) {
                    Text("根据设备名称自动生成广播数据")
                }
            }

            // 广播数据预览
            item {
                if (device.advDataHex.isNotEmpty()) {
                    AdvDataPreview(advDataHex = device.advDataHex)
                }
            }

            // 广播间隔
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "广播间隔",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("100 ms", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${device.intervalMs} ms",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("10000 ms", style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = device.intervalMs.toFloat(),
                            onValueChange = { viewModel.updateInterval(it.toLong()) },
                            valueRange = 100f..10000f,
                            steps = 98
                        )
                        Text(
                            "设备出现在扫描结果中的频率",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 说明文本
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "提示",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 广播数据是BLE设备发送的原始数据包\n" +
                                    "• 标准格式包含Flags和设备名称\n" +
                                    "• RSSI值越接近0信号越强\n" +
                                    "• 间隔越短设备更新越频繁（耗电）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvDataPreview(advDataHex: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "广播数据预览",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 按字节显示
            val bytes = advDataHex.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bytes.chunked(8).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { byte ->
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = byte,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 解析AD结构
            Text(
                text = parseAdvData(advDataHex),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * 简单解析广播数据结构
 */
fun parseAdvData(hexString: String): String {
    if (hexString.length < 6) return "数据太短"

    val result = mutableListOf<String>()
    val bytes = hexString.chunked(2).map { it.toInt(16) }

    var i = 0
    while (i < bytes.size) {
        if (i + 1 >= bytes.size) break

        val length = bytes[i]
        if (length == 0 || i + length >= bytes.size) break

        val type = bytes[i + 1]
        val typeName = when (type) {
            0x01 -> "Flags"
            0x02 -> "Incomplete 16-bit UUIDs"
            0x03 -> "Complete 16-bit UUIDs"
            0x08 -> "Shortened Local Name"
            0x09 -> "Complete Local Name"
            0x0A -> "Tx Power Level"
            0x16 -> "Service Data"
            0xFF -> "Manufacturer Data"
            else -> "Type 0x${type.toString(16).uppercase()}"
        }

        result.add("• $typeName ($length bytes)")

        // 尝试解析名称
        if (type == 0x08 || type == 0x09) {
            val nameBytes = bytes.subList(i + 2, minOf(i + 1 + length, bytes.size))
            val name = nameBytes.map { it.toChar() }.joinToString("")
            result.add("  名称: $name")
        }

        i += length + 1
    }

    return result.joinToString("\n")
}
