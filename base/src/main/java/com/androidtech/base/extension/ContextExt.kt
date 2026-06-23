package com.androidtech.base.extension

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.androidtech.base.permission.Permission
import androidx.core.net.toUri
import java.security.MessageDigest


fun Context.isPermissionGranted(permission: Permission): Boolean =
    when (permission) {
        Permission.DisplayOverlay -> {
            Settings.canDrawOverlays(this)
        }

        Permission.PostNotification -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        else -> false
    }

fun Context.isServiceRunning(serviceName: String): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    manager.getRunningServices(Int.MAX_VALUE).forEach { serviceInfo ->
        if (serviceName == serviceInfo.service.className) {
            true
        }
    }
    return false
}

fun Context.requestSystemAlertWindowPermission() {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${packageName}".toUri()
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.requestUsageStatsPermission() {
    startActivity(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}

fun Context.hasUsageStatsPermission(): Boolean {
    val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@SuppressLint("HardwareIds")
fun Context.getTestDevice(): String {
    val androidId = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ANDROID_ID
    )

    return MessageDigest.getInstance("MD5")
        .digest(androidId.toByteArray())
        .joinToString("") { "%02X".format(it) }
}