# BluetoothHook - 蓝牙虚拟设备注入模块

<div align="center">

[![GitHub release](https://img.shields.io/github/v/release/jingyu233/bluetoothhook?include_prereleases&style=flat-square)](https://github.com/jingyu233/bluetoothhook/releases)
[![License](https://img.shields.io/github/license/jingyu233/bluetoothhook?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B-green?style=flat-square&logo=android)](https://www.android.com)
[![Stars](https://img.shields.io/github/stars/jingyu233/bluetoothhook?style=flat-square)](https://github.com/jingyu233/bluetoothhook/stargazers)
[![Issues](https://img.shields.io/github/issues/jingyu233/bluetoothhook?style=flat-square)](https://github.com/jingyu233/bluetoothhook/issues)

[简体中文](#简体中文) | [English](#english)

</div>

---

# 简体中文

> ⚠️ **重要声明**
> 本项目仅供学习研究和合法的开发调试使用。严禁用于任何非法目的，包括但不限于：欺骗、破解商业软件、干扰他人设备、未经授权的追踪、恶意攻击等。使用本模块造成的任何法律后果由使用者自行承担。请遵守当地法律法规。

一个强大的 Xposed/LSPosed 模块，用于向 Android 系统蓝牙扫描结果中注入虚拟 BLE 设备，方便开发者进行蓝牙应用调试。

## ✨ 功能特性

### 核心功能
- ✅ **虚拟设备注入** - 向系统蓝牙扫描结果注入自定义 BLE 设备
- ✅ **完全自定义** - 自定义 MAC 地址、RSSI 信号强度和广播数据
- ✅ **多设备管理** - 同时管理多个虚拟设备
- ✅ **实时生效** - 修改设备配置后无需重启

## 📋 系统要求

- Android 10 (API 29) 及以上
- LSPosed 框架（推荐）或 Xposed 框架
- Root 权限

## 📱 使用指南

### 添加虚拟设备

1. 点击右下角的 **+** 按钮
2. 填写设备信息：
   - **设备名称**: 显示在扫描列表中的名称
   - **MAC 地址**: 格式为 `AA:BB:CC:DD:EE:FF`
   - **RSSI**: 信号强度 (-100 到 0，越接近 0 信号越强)
   - **广播数据**: 十六进制格式的 BLE 广播数据
   - **扫描响应数据** (可选): 扫描响应的十六进制数据
   - **广播间隔**: 设备出现频率（毫秒）
3. 点击右上角的 **√** 保存

### 广播模式选择

系统会根据数据长度自动选择最佳模式：

**传统广播模式** (1-31 字节)
- 适用于简单设备
- 兼容所有 BLE 设备
- 示例: `02 01 06 09 09 4D79426561636F6E` (Flags + 设备名 "MyBeacon")

**传统广播 + 扫描响应** (32-62 字节)
- 将数据分为广播数据（31字节）和扫描响应（31字节）
- 兼容所有 BLE 设备
- 点击 **"自动分割"** 按钮可自动分割数据

**扩展广播模式** (63-254 字节)
- 支持大量数据传输
- 需要 BLE 5.0+ 硬件支持
- 点击 **"使用扩展广播"** 按钮切换

### 快速创建标准设备

点击 "根据设备名称自动生成广播数据" 按钮，系统会自动生成包含 Flags 和设备名称的标准 BLE 广播数据。

## 🔧 技术架构

### Hook 实现

模块通过 Hook `ScanController.onScanResultInternal()` 方法（位于 `com.android.bluetooth.le_scan.ScanController`）实现虚拟设备注入。

**关键 Hook 点**（基于 AOSP 源码分析）：
- **文件**: `com/android/bluetooth/le_scan/ScanController.java`
- **方法**: `onScanResultInternal()` (line 362)
- **注入时机**: 在真实扫描结果分发后，遍历所有扫描客户端时注入

## 📊 广播数据格式

BLE 广播数据使用 AD (Advertising Data) 结构格式：

```
[Length] [Type] [Data...] [Length] [Type] [Data...]
```

## 📄 开源协议

本项目采用 MIT 协议开源。

## 🙏 鸣谢

- [LSPosed](https://github.com/LSPosed/LSPosed) - 强大的 Xposed 框架
- [Xposed Framework](https://github.com/rovo89/Xposed) - 原始 Xposed 框架
- AOSP Bluetooth Stack - 蓝牙协议栈源码参考

## 📞 反馈与支持

如果你遇到问题或有建议，欢迎提交 Issue 或 Pull Request。

---

**免责声明**: 本模块仅用于学习研究和合法的开发调试目的。严禁用于欺骗、攻击、未经授权访问等任何非法用途。使用本模块时请确保：
1. 遵守当地法律法规
2. 获得设备所有者的明确授权
3. 不干扰他人设备或服务的正常运行
4. 不用于商业欺诈或侵犯他人隐私

违法使用造成的一切法律后果由使用者自行承担，项目作者不承担任何责任。

---

# English

> ⚠️ **Important Notice**
> This project is for educational and legitimate development/debugging purposes ONLY. It is strictly prohibited to use for any illegal purposes, including but not limited to: deception, cracking commercial software, interfering with others' devices, unauthorized tracking, malicious attacks, etc. Users are solely responsible for any legal consequences arising from the use of this module. Please comply with local laws and regulations.

A powerful Xposed/LSPosed module for injecting virtual BLE devices into Android system Bluetooth scan results, facilitating Bluetooth application debugging for developers.

## ✨ Features

### Core Features
- ✅ **Virtual Device Injection** - Inject custom BLE devices into system Bluetooth scan results
- ✅ **Fully Customizable** - Customize MAC address, RSSI signal strength, and advertising data
- ✅ **Multi-Device Management** - Manage multiple virtual devices simultaneously
- ✅ **Real-time Effect** - Changes take effect without restart

## 📋 System Requirements

- Android 10 (API 29) and above
- LSPosed Framework (recommended) or Xposed Framework
- Root access

## 📱 User Guide

### Adding Virtual Devices

1. Tap the **+** button in the bottom right corner
2. Fill in device information:
   - **Device Name**: Name displayed in scan list
   - **MAC Address**: Format `AA:BB:CC:DD:EE:FF`
   - **RSSI**: Signal strength (-100 to 0, closer to 0 means stronger)
   - **Advertising Data**: BLE advertising data in hexadecimal format
   - **Scan Response Data** (optional): Hexadecimal scan response data
   - **Advertising Interval**: Device appearance frequency (milliseconds)
3. Tap the **√** button in the top right to save

### Advertising Mode Selection

The system automatically selects the best mode based on data length:

**Legacy Advertising Mode** (1-31 bytes)
- Suitable for simple devices
- Compatible with all BLE devices
- Example: `02 01 06 09 09 4D79426561636F6E` (Flags + device name "MyBeacon")

**Legacy Advertising + Scan Response** (32-62 bytes)
- Data split into advertising data (31 bytes) and scan response (31 bytes)
- Compatible with all BLE devices
- Tap **"Auto Split"** button to automatically split data

**Extended Advertising Mode** (63-254 bytes)
- Supports large data transmission
- Requires BLE 5.0+ hardware support
- Tap **"Use Extended Advertising"** button to switch

### Quick Create Standard Device

Tap the "Auto-generate advertising data from device name" button, and the system will automatically generate standard BLE advertising data containing Flags and device name.

## 🔧 Technical Architecture

### Hook Implementation

The module implements virtual device injection by hooking the `ScanController.onScanResultInternal()` method (located in `com.android.bluetooth.le_scan.ScanController`).

**Key Hook Point** (based on AOSP source code analysis):
- **File**: `com/android/bluetooth/le_scan/ScanController.java`
- **Method**: `onScanResultInternal()` (line 362)
- **Injection Timing**: After real scan results are distributed, inject when iterating through all scan clients

## 📊 Advertising Data Format

BLE advertising data uses AD (Advertising Data) structure format:

```
[Length] [Type] [Data...] [Length] [Type] [Data...]
```

## 📄 License

This project is licensed under the MIT License.

## 🙏 Acknowledgments

- [LSPosed](https://github.com/LSPosed/LSPosed) - Powerful Xposed framework
- [Xposed Framework](https://github.com/rovo89/Xposed) - Original Xposed framework
- AOSP Bluetooth Stack - Bluetooth stack source code reference

## 📞 Feedback & Support

If you encounter any issues or have suggestions, feel free to submit an Issue or Pull Request.

---

**Disclaimer**: This module is for educational and legitimate development/debugging purposes only. It is strictly prohibited to use for deception, attacks, unauthorized access, or any other illegal purposes. When using this module, please ensure:
1. Compliance with local laws and regulations
2. Explicit authorization from device owners
3. No interference with others' devices or services
4. No use for commercial fraud or privacy infringement

All legal consequences arising from illegal use are the sole responsibility of the user, and the project author assumes no liability.
