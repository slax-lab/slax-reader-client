package com.slax.reader.utils

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.posix.uname
import platform.posix.utsname

enum class DeviceModel {
    iPhone,
    iPhone_3G,
    iPhone_3GS,
    iPhone_4,
    iPhone_4S,
    iPhone_5,
    iPhone_5C,
    iPhone_5S,
    iPhone_6,
    iPhone_6_Plus,
    iPhone_6S,
    iPhone_6S_Plus,
    iPhone_SE,
    iPhone_7,
    iPhone_7_Plus,
    iPhone_8,
    iPhone_8_Plus,
    iPhone_X,
    iPhone_XS,
    iPhone_XS_Max,
    iPhone_XR,
    iPhone_11,
    iPhone_11_Pro,
    iPhone_11_Pro_Max,
    iPhone_SE_2,
    iPhone_12_mini,
    iPhone_12,
    iPhone_12_Pro,
    iPhone_12_Pro_Max,
    iPhone_13_mini,
    iPhone_13,
    iPhone_13_Pro,
    iPhone_13_Pro_Max,
    iPhone_SE_3,
    iPhone_14,
    iPhone_14_Plus,
    iPhone_14_Pro,
    iPhone_14_Pro_Max,
    iPhone_15,
    iPhone_15_Plus,
    iPhone_15_Pro,
    iPhone_15_Pro_Max,
    iPhone_16,
    iPhone_16_Plus,
    iPhone_16_Pro,
    iPhone_16_Pro_Max,
    iPhone_16e,
    iPhone_17_Pro,
    iPhone_17_Pro_Max,
    iPhone_17,
    iPhone_Air,
}

enum class DeviceType {
    iPHONE,
    iPAD,
    iPod,
    AppleTV,
    AppleWatch,
    HomePod,
    Simulator
}

enum class ScreenType {
    iPhone_XR_11,
    iPhone_X_XS_11Pro,
    iPhone_XSMax_11ProMax,
    iPhone_12mini_13mini,
    iPhone_12_12Pro_13_13Pro_14,
    iPhone_12ProMax_13ProMax_14Plus,
    iPhone_14Pro_15_15Pro_16,
    iPhone_14ProMax_15Plus_15ProMax_16Plus,
    iPhone_16Pro_17_17Pro,
    iPhone_16ProMax_17ProMax,
    iPhone_Air,
    Unknown
}

fun getDeviceType(): DeviceType {
    val deviceName = getDeviceName()
    return when {
        deviceName.startsWith("iPhone") -> DeviceType.iPHONE
        deviceName.startsWith("iPad") -> DeviceType.iPAD
        deviceName.startsWith("iPod") -> DeviceType.iPod
        deviceName.startsWith("AppleTV") -> DeviceType.AppleTV
        deviceName.startsWith("Watch") -> DeviceType.AppleWatch
        deviceName.startsWith("HomePod") -> DeviceType.HomePod
        else -> DeviceType.Simulator
    }
}

@OptIn(ExperimentalForeignApi::class)
fun getDeviceModel(): DeviceModel {
    val deviceName = getDeviceName()
    return when (deviceName) {
        "iPhone1,1" -> DeviceModel.iPhone
        "iPhone1,2" -> DeviceModel.iPhone_3G
        "iPhone2,1" -> DeviceModel.iPhone_3GS
        "iPhone3,1", "iPhone3,2", "iPhone3,3" -> DeviceModel.iPhone_4
        "iPhone4,1", "iPhone4,2", "iPhone4,3" -> DeviceModel.iPhone_4S
        "iPhone5,1", "iPhone5,2" -> DeviceModel.iPhone_5
        "iPhone5,3", "iPhone5,4" -> DeviceModel.iPhone_5C
        "iPhone6,1", "iPhone6,2" -> DeviceModel.iPhone_5S
        "iPhone7,2" -> DeviceModel.iPhone_6
        "iPhone7,1" -> DeviceModel.iPhone_6_Plus
        "iPhone8,1" -> DeviceModel.iPhone_6S
        "iPhone8,2" -> DeviceModel.iPhone_6S_Plus
        "iPhone8,4" -> DeviceModel.iPhone_SE
        "iPhone9,1", "iPhone9,3" -> DeviceModel.iPhone_7
        "iPhone9,2", "iPhone9,4" -> DeviceModel.iPhone_7_Plus
        "iPhone10,1", "iPhone10,4" -> DeviceModel.iPhone_8
        "iPhone10,2", "iPhone10,5" -> DeviceModel.iPhone_8_Plus
        "iPhone10,3", "iPhone10,6" -> DeviceModel.iPhone_X
        "iPhone11,2" -> DeviceModel.iPhone_XS
        "iPhone11,4", "iPhone11,6" -> DeviceModel.iPhone_XS_Max
        "iPhone11,8" -> DeviceModel.iPhone_XR
        "iPhone12,1" -> DeviceModel.iPhone_11
        "iPhone12,3" -> DeviceModel.iPhone_11_Pro
        "iPhone12,5" -> DeviceModel.iPhone_11_Pro_Max
        "iPhone12,8" -> DeviceModel.iPhone_SE_2
        "iPhone13,1" -> DeviceModel.iPhone_12_mini
        "iPhone13,2" -> DeviceModel.iPhone_12
        "iPhone13,3" -> DeviceModel.iPhone_12_Pro
        "iPhone13,4" -> DeviceModel.iPhone_12_Pro_Max
        "iPhone14,4" -> DeviceModel.iPhone_13_mini
        "iPhone14,5" -> DeviceModel.iPhone_13
        "iPhone14,2" -> DeviceModel.iPhone_13_Pro
        "iPhone14,3" -> DeviceModel.iPhone_13_Pro_Max
        "iPhone14,6" -> DeviceModel.iPhone_SE_3
        "iPhone14,7" -> DeviceModel.iPhone_14
        "iPhone14,8" -> DeviceModel.iPhone_14_Plus
        "iPhone15,2" -> DeviceModel.iPhone_14_Pro
        "iPhone15,3" -> DeviceModel.iPhone_14_Pro_Max
        "iPhone15,4" -> DeviceModel.iPhone_15
        "iPhone15,5" -> DeviceModel.iPhone_15_Plus
        "iPhone16,1" -> DeviceModel.iPhone_15_Pro
        "iPhone16,2" -> DeviceModel.iPhone_15_Pro_Max
        "iPhone17,3" -> DeviceModel.iPhone_16
        "iPhone17,4" -> DeviceModel.iPhone_16_Plus
        "iPhone17,1" -> DeviceModel.iPhone_16_Pro
        "iPhone17,2" -> DeviceModel.iPhone_16_Pro_Max
        "iPhone17,5" -> DeviceModel.iPhone_16e
        "iPhone18,1" -> DeviceModel.iPhone_17_Pro
        "iPhone18,2" -> DeviceModel.iPhone_17_Pro_Max
        "iPhone18,3" -> DeviceModel.iPhone_17
        "iPhone18,4" -> DeviceModel.iPhone_Air
        else -> DeviceModel.iPhone
    }
}

fun getScreenType(): ScreenType {
    return when (getDeviceModel()) {
        DeviceModel.iPhone_XR, DeviceModel.iPhone_11 -> ScreenType.iPhone_XR_11
        DeviceModel.iPhone_X, DeviceModel.iPhone_XS, DeviceModel.iPhone_11_Pro -> ScreenType.iPhone_X_XS_11Pro
        DeviceModel.iPhone_XS_Max, DeviceModel.iPhone_11_Pro_Max -> ScreenType.iPhone_XSMax_11ProMax
        DeviceModel.iPhone_12_mini, DeviceModel.iPhone_13_mini -> ScreenType.iPhone_12mini_13mini
        DeviceModel.iPhone_12, DeviceModel.iPhone_12_Pro, DeviceModel.iPhone_13, DeviceModel.iPhone_13_Pro, DeviceModel.iPhone_14 -> ScreenType.iPhone_12_12Pro_13_13Pro_14
        DeviceModel.iPhone_12_Pro_Max, DeviceModel.iPhone_13_Pro_Max, DeviceModel.iPhone_14_Plus -> ScreenType.iPhone_12ProMax_13ProMax_14Plus
        DeviceModel.iPhone_14_Pro, DeviceModel.iPhone_15, DeviceModel.iPhone_15_Pro, DeviceModel.iPhone_16, DeviceModel.iPhone_16e -> ScreenType.iPhone_14Pro_15_15Pro_16
        DeviceModel.iPhone_14_Pro_Max, DeviceModel.iPhone_15_Plus, DeviceModel.iPhone_15_Pro_Max, DeviceModel.iPhone_16_Plus -> ScreenType.iPhone_14ProMax_15Plus_15ProMax_16Plus
        DeviceModel.iPhone_16_Pro, DeviceModel.iPhone_17, DeviceModel.iPhone_17_Pro -> ScreenType.iPhone_16Pro_17_17Pro
        DeviceModel.iPhone_16_Pro_Max, DeviceModel.iPhone_17_Pro_Max -> ScreenType.iPhone_16ProMax_17ProMax
        DeviceModel.iPhone_Air -> ScreenType.iPhone_Air
        else -> ScreenType.Unknown
    }
}

fun isNotchOrDynamicIsland(): Boolean {
    return when (getScreenType()) {
        ScreenType.iPhone_XR_11,
        ScreenType.iPhone_X_XS_11Pro,
        ScreenType.iPhone_XSMax_11ProMax,
        ScreenType.iPhone_12mini_13mini,
        ScreenType.iPhone_12_12Pro_13_13Pro_14,
        ScreenType.iPhone_12ProMax_13ProMax_14Plus,
        ScreenType.iPhone_14Pro_15_15Pro_16,
        ScreenType.iPhone_14ProMax_15Plus_15ProMax_16Plus,
        ScreenType.iPhone_16Pro_17_17Pro,
        ScreenType.iPhone_16ProMax_17ProMax,
        ScreenType.iPhone_Air -> true

        else -> false
    }
}

@OptIn(ExperimentalForeignApi::class)
fun isDynamicIsland(): Boolean {
    return when (getScreenType()) {
        ScreenType.iPhone_14Pro_15_15Pro_16,
        ScreenType.iPhone_14ProMax_15Plus_15ProMax_16Plus,
        ScreenType.iPhone_16Pro_17_17Pro,
        ScreenType.iPhone_16ProMax_17ProMax -> true

        else -> false
    }
}

@OptIn(ExperimentalForeignApi::class)
fun getDeviceName(): String {
    return memScoped {
        val systemInfo = alloc<utsname>()
        uname(systemInfo.ptr)
        systemInfo.machine.toKString()
    }
}


@OptIn(ExperimentalForeignApi::class)
fun getNotchFrame(): CValue<CGRect>? {
    return when (getScreenType()) {
        ScreenType.iPhone_XR_11 ->
            CGRectMake(x = 91.0, y = 0.0, width = 232.0, height = 33.0)

        ScreenType.iPhone_X_XS_11Pro ->
            CGRectMake(x = 83.33, y = 0.0, width = 209.0, height = 29.67)

        ScreenType.iPhone_XSMax_11ProMax ->
            CGRectMake(x = 102.0, y = 0.0, width = 209.0, height = 29.67)

        ScreenType.iPhone_12mini_13mini ->
            CGRectMake(x = 100.5, y = 0.0, width = 174.0, height = 34.33)

        ScreenType.iPhone_12_12Pro_13_13Pro_14 ->
            CGRectMake(x = 114.0, y = 0.0, width = 162.0, height = 32.0)

        ScreenType.iPhone_12ProMax_13ProMax_14Plus ->
            CGRectMake(x = 133.0, y = 0.0, width = 162.0, height = 32.0)

        ScreenType.iPhone_14Pro_15_15Pro_16 ->
            CGRectMake(x = 133.67, y = 11.33, width = 125.67, height = 36.67)

        ScreenType.iPhone_14ProMax_15Plus_15ProMax_16Plus ->
            CGRectMake(x = 152.0, y = 11.33, width = 125.67, height = 36.67)

        ScreenType.iPhone_16Pro_17_17Pro ->
            CGRectMake(x = 138.0, y = 14.0, width = 125.67, height = 36.67)

        ScreenType.iPhone_16ProMax_17ProMax ->
            CGRectMake(x = 157.0, y = 14.0, width = 126.0, height = 36.67)

        ScreenType.iPhone_Air ->
            CGRectMake(x = 147.0, y = 20.0, width = 125.0, height = 36.0)

        else -> null
    }
}
