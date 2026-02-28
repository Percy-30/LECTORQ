package com.scannerpro.lectorqr.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

enum class AppIcon(val componentName: String) {
    DEFAULT(".MainActivityDefault"),
    GOLD(".MainActivityGold"),
    DARK(".MainActivityDark"),
    NEON(".MainActivityNeon")
}

class IconManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun changeIcon(newIcon: AppIcon) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        AppIcon.values().forEach { icon ->
            val state = if (icon == newIcon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            packageManager.setComponentEnabledSetting(
                ComponentName(packageName, "$packageName${icon.componentName}"),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun getCurrentIcon(): AppIcon {
        val packageName = context.packageName
        val packageManager = context.packageManager

        AppIcon.values().forEach { icon ->
            val state = packageManager.getComponentEnabledSetting(
                ComponentName(packageName, "$packageName${icon.componentName}")
            )
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return icon
            }
        }
        return AppIcon.DEFAULT
    }
}
