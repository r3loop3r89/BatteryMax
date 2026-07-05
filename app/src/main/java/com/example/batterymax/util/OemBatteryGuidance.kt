package com.example.batterymax.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

data class OemGuidanceAction(
    val label: String,
    val intents: List<Intent>
)

data class OemBatteryGuidance(
    val oem: DeviceOem,
    val summary: String,
    val steps: List<String>,
    val actions: List<OemGuidanceAction>
)

fun getOemBatteryGuidance(context: Context): OemBatteryGuidance? {
    val oem = detectDeviceOem() ?: return null
    return OemBatteryGuidance(
        oem = oem,
        summary = summaryFor(oem),
        steps = stepsFor(oem),
        actions = actionsFor(context, oem).filter { action ->
            action.intents.any { intent -> isIntentResolvable(context, intent) }
        }
    )
}

fun openOemGuidanceAction(context: Context, action: OemGuidanceAction): Boolean =
    openFirstResolvableIntent(context, action.intents)

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun openFirstResolvableIntent(context: Context, intents: List<Intent>): Boolean {
    for (intent in intents) {
        if (isIntentResolvable(context, intent)) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        }
    }
    return false
}

private fun isIntentResolvable(context: Context, intent: Intent): Boolean =
    context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null

private fun componentIntent(packageName: String, className: String): Intent =
    Intent().setComponent(ComponentName(packageName, className))

private fun actionIntent(action: String): Intent = Intent(action)

private fun actionsFor(context: Context, oem: DeviceOem): List<OemGuidanceAction> = when (oem) {
    DeviceOem.ONEPLUS -> listOf(
        OemGuidanceAction(
            label = "Open auto-launch settings",
            intents = listOf(
                componentIntent(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                ),
                actionIntent("com.android.settings.action.BACKGROUND_OPTIMIZE")
            )
        ),
        OemGuidanceAction(
            label = "Open battery settings",
            intents = listOf(
                actionIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                actionIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            )
        )
    )
    DeviceOem.SAMSUNG -> listOf(
        OemGuidanceAction(
            label = "Open battery settings",
            intents = listOf(
                componentIntent(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                ),
                componentIntent(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"
                ),
                componentIntent(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity"
                )
            )
        ),
        OemGuidanceAction(
            label = "Open sleeping apps",
            intents = listOf(
                componentIntent(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity"
                )
            )
        )
    )
    DeviceOem.XIAOMI -> listOf(
        OemGuidanceAction(
            label = "Open autostart settings",
            intents = listOf(
                componentIntent(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            )
        ),
        OemGuidanceAction(
            label = "Open battery saver",
            intents = listOf(
                componentIntent(
                    "com.miui.securitycenter",
                    "com.miui.powercenter.PowerSettings"
                ),
                actionIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            )
        )
    )
    DeviceOem.OPPO, DeviceOem.REALME -> listOf(
        OemGuidanceAction(
            label = "Open startup manager",
            intents = listOf(
                componentIntent(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                ),
                componentIntent(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                ),
                componentIntent(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            )
        ),
        OemGuidanceAction(
            label = "Open app battery",
            intents = listOf(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        )
    )
    DeviceOem.VIVO -> listOf(
        OemGuidanceAction(
            label = "Open background startup",
            intents = listOf(
                componentIntent(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                ),
                componentIntent(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ),
                componentIntent(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            )
        )
    )
    DeviceOem.HUAWEI -> listOf(
        OemGuidanceAction(
            label = "Open startup manager",
            intents = listOf(
                componentIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                ),
                componentIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            )
        )
    )
    DeviceOem.HONOR -> listOf(
        OemGuidanceAction(
            label = "Open protected apps",
            intents = listOf(
                componentIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                ),
                componentIntent(
                    "com.hihonor.systemmanager",
                    "com.hihonor.systemmanager.optimize.process.ProtectActivity"
                )
            )
        )
    )
    DeviceOem.ASUS -> listOf(
        OemGuidanceAction(
            label = "Open autostart settings",
            intents = listOf(
                componentIntent(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.autostart.AutoStartActivity"
                ),
                componentIntent(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.powersaver.PowerSaverSettings"
                ),
                componentIntent(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.entry.FunctionActivity"
                ).setData(Uri.parse("mobilemanager://function/entry/AutoStart"))
            )
        )
    )
    DeviceOem.MOTOROLA -> listOf(
        OemGuidanceAction(
            label = "Open battery settings",
            intents = listOf(
                actionIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                actionIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            )
        )
    )
    DeviceOem.LENOVO -> listOf(
        OemGuidanceAction(
            label = "Open battery settings",
            intents = listOf(
                actionIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                actionIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            )
        )
    )
    DeviceOem.MEIZU -> listOf(
        OemGuidanceAction(
            label = "Open background settings",
            intents = listOf(
                actionIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                actionIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            )
        )
    )
    DeviceOem.NOKIA -> listOf(
        OemGuidanceAction(
            label = "Open power exceptions",
            intents = listOf(
                componentIntent(
                    "com.evenwell.powersaving.g3",
                    "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
                )
            )
        )
    )
}

private fun summaryFor(oem: DeviceOem): String = when (oem) {
    DeviceOem.ONEPLUS ->
        "${oem.displayName} adds extra background limits beyond Android. Adjust these settings " +
            "so monitoring keeps sampling reliably."
    DeviceOem.SAMSUNG ->
        "Samsung may put unused apps to sleep and restrict background work. Whitelist Battery Max " +
            "for steadier battery history."
    DeviceOem.XIAOMI ->
        "MIUI restricts background apps by default. Enable autostart and remove battery " +
            "restrictions for Battery Max."
    DeviceOem.OPPO, DeviceOem.REALME ->
        "ColorOS can block background activity. Allow Battery Max to start in the background " +
            "and remove battery restrictions."
    DeviceOem.VIVO ->
        "Vivo/iQOO may block background startup. Allow Battery Max to run in the background."
    DeviceOem.HUAWEI, DeviceOem.HONOR ->
        "Huawei/Honor protect background apps aggressively. Add Battery Max to protected/startup apps."
    DeviceOem.ASUS ->
        "Asus mobile manager can limit autostart. Allow Battery Max to run in the background."
    DeviceOem.MOTOROLA ->
        "Motorola battery saver can limit background apps. Keep Battery Max unrestricted."
    DeviceOem.LENOVO ->
        "Lenovo battery management may pause background apps. Keep Battery Max unrestricted."
    DeviceOem.MEIZU ->
        "Meizu battery management may pause background apps. Keep Battery Max unrestricted."
    DeviceOem.NOKIA ->
        "Nokia/HMD power saving can restrict background apps. Add Battery Max to power exceptions."
}

private fun stepsFor(oem: DeviceOem): List<String> = when (oem) {
    DeviceOem.ONEPLUS -> listOf(
        "Settings → Battery → Battery optimization → All apps → Battery Max → Don't optimize",
        "Settings → Battery → Advanced optimization → turn off Deep optimization and Sleep standby optimization",
        "Disable App auto-launch for Battery Max, or allow background activity",
        "Lock Battery Max in Recents (long-press the app card → Lock)"
    )
    DeviceOem.SAMSUNG -> listOf(
        "Settings → Apps → Battery Max → Battery → Unrestricted / Don't optimize",
        "Settings → Battery → Background usage limits → remove from Sleeping and Deep sleeping lists",
        "Add Battery Max to Never sleeping apps if available",
        "Turn off Adaptive battery and Put unused apps to sleep",
        "Lock Battery Max in Recents if your phone supports it"
    )
    DeviceOem.XIAOMI -> listOf(
        "Settings → Apps → Battery Max → Battery saver → No restrictions",
        "Security → Permissions → Autostart → enable Battery Max",
        "Security → Battery → App battery saver → Battery Max → No restriction",
        "Lock Battery Max in Recents"
    )
    DeviceOem.OPPO, DeviceOem.REALME -> listOf(
        "Settings → Battery → Battery Max → Don't optimize / Allow background activity",
        "Settings → Apps → Battery Max → Allow auto-launch and background activity",
        "Startup manager → allow Battery Max",
        "Lock Battery Max in Recents"
    )
    DeviceOem.VIVO -> listOf(
        "i Manager / Settings → Battery → Background power consumption → allow Battery Max",
        "Settings → More settings → Applications → Autostart → enable Battery Max",
        "Background startup manager → allow Battery Max",
        "Lock Battery Max in Recents"
    )
    DeviceOem.HUAWEI -> listOf(
        "Phone Manager → Startup apps → allow Battery Max",
        "Phone Manager → Protected apps → enable Battery Max",
        "Settings → Battery → App launch → Battery Max → Manage manually, enable all toggles",
        "Lock Battery Max in Recents"
    )
    DeviceOem.HONOR -> listOf(
        "Phone Manager → Startup apps → allow Battery Max",
        "Phone Manager → Protected apps → enable Battery Max",
        "Settings → Battery → App launch → Battery Max → Manage manually",
        "Lock Battery Max in Recents"
    )
    DeviceOem.ASUS -> listOf(
        "Mobile Manager → Autostart → allow Battery Max",
        "Settings → Battery → Battery Max → Don't optimize",
        "Lock Battery Max in Recents"
    )
    DeviceOem.MOTOROLA -> listOf(
        "Settings → Apps → Battery Max → Battery → Unrestricted",
        "Settings → Battery → Battery optimization → Battery Max → Not optimized",
        "Disable aggressive battery saver modes if enabled"
    )
    DeviceOem.LENOVO -> listOf(
        "Settings → Battery → Battery optimization → Battery Max → Don't optimize",
        "Settings → Apps → Battery Max → Battery → Unrestricted / Allow background activity",
        "Disable any auto-clean or background restriction features for Battery Max"
    )
    DeviceOem.MEIZU -> listOf(
        "Security → Permissions → Auto-launch → enable Battery Max",
        "Settings → Battery → Battery Max → Allow background activity",
        "Lock Battery Max in Recents"
    )
    DeviceOem.NOKIA -> listOf(
        "Settings → Battery → Battery optimization → All apps → Battery Max → Don't optimize",
        "Evenwell Power Saving → Exceptions → add Battery Max",
        "Disable adaptive battery features that restrict background apps"
    )
}