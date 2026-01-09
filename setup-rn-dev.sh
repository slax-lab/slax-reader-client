#!/bin/bash

# React Native 开发辅助脚本

echo "正在配置 Android 开发环境..."

# 检查 adb 是否可用
if ! command -v adb &> /dev/null; then
    echo "❌ adb 未找到，请确保 Android SDK 已安装并添加到 PATH"
    echo "   通常位于: ~/Library/Android/sdk/platform-tools/adb"
    exit 1
fi

# 设置端口转发
echo "设置端口转发 (Metro Bundler: 8081)..."
adb reverse tcp:8081 tcp:8081

if [ $? -eq 0 ]; then
    echo "✅ 端口转发配置成功"
    echo ""
    echo "现在可以："
    echo "1. 重新打开应用"
    echo "2. 点击 Sidebar 中的 'React Native Demo'"
    echo ""
    echo "如果还有问题，尝试："
    echo "- 摇晃设备打开开发菜单"
    echo "- 选择 'Reload'"
else
    echo "❌ 端口转发配置失败"
    echo ""
    echo "请确保："
    echo "1. 设备已通过 USB 连接（或模拟器正在运行）"
    echo "2. USB 调试已启用"
    echo "3. 运行 'adb devices' 查看设备状态"
fi
