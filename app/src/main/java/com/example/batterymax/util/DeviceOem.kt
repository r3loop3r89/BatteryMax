package com.example.batterymax.util

import android.os.Build
import java.util.Locale

enum class DeviceOem(val displayName: String) {
    ONEPLUS("OnePlus"),
    SAMSUNG("Samsung"),
    XIAOMI("Xiaomi"),
    OPPO("Oppo"),
    REALME("Realme"),
    VIVO("Vivo"),
    HUAWEI("Huawei"),
    HONOR("Honor"),
    ASUS("Asus"),
    MOTOROLA("Motorola"),
    LENOVO("Lenovo"),
    MEIZU("Meizu"),
    NOKIA("Nokia");

    val settingsCardTitle: String
        get() = "$displayName background settings"
}

fun detectDeviceOem(): DeviceOem? {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
    val brand = Build.BRAND.lowercase(Locale.ROOT)
    val tokens = setOf(manufacturer, brand)

    return when {
        tokens.any { it in ONEPLUS_IDS } -> DeviceOem.ONEPLUS
        tokens.any { it in SAMSUNG_IDS } -> DeviceOem.SAMSUNG
        tokens.any { it in XIAOMI_IDS } -> DeviceOem.XIAOMI
        tokens.any { it in OPPO_IDS } -> DeviceOem.OPPO
        tokens.any { it in REALME_IDS } -> DeviceOem.REALME
        tokens.any { it in VIVO_IDS } -> DeviceOem.VIVO
        tokens.any { it in HUAWEI_IDS } -> DeviceOem.HUAWEI
        tokens.any { it in HONOR_IDS } -> DeviceOem.HONOR
        tokens.any { it in ASUS_IDS } -> DeviceOem.ASUS
        tokens.any { it in MOTOROLA_IDS } -> DeviceOem.MOTOROLA
        tokens.any { it in LENOVO_IDS } -> DeviceOem.LENOVO
        tokens.any { it in MEIZU_IDS } -> DeviceOem.MEIZU
        tokens.any { it in NOKIA_IDS } -> DeviceOem.NOKIA
        else -> null
    }
}

private val ONEPLUS_IDS = setOf("oneplus")
private val SAMSUNG_IDS = setOf("samsung")
private val XIAOMI_IDS = setOf("xiaomi", "redmi", "poco")
private val OPPO_IDS = setOf("oppo")
private val REALME_IDS = setOf("realme")
private val VIVO_IDS = setOf("vivo", "iqoo")
private val HUAWEI_IDS = setOf("huawei")
private val HONOR_IDS = setOf("honor")
private val ASUS_IDS = setOf("asus")
private val MOTOROLA_IDS = setOf("motorola", "moto")
private val LENOVO_IDS = setOf("lenovo")
private val MEIZU_IDS = setOf("meizu")
private val NOKIA_IDS = setOf("nokia", "hmd global")